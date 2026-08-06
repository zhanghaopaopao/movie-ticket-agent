package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.dto.OrderSnackItemDTO;
import com.szml.movieticket.dto.OrderSnackSelectionDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.entity.OrderItem;
import com.szml.movieticket.entity.OrderSnackItem;
import com.szml.movieticket.entity.Payment;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.entity.SnackProduct;
import com.szml.movieticket.entity.TicketOrder;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.OrderException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.OrderItemMapper;
import com.szml.movieticket.mapper.OrderMapper;
import com.szml.movieticket.mapper.OrderSnackItemMapper;
import com.szml.movieticket.mapper.PaymentMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.mapper.SnackProductMapper;
import com.szml.movieticket.service.OrderSnackService;
import com.szml.movieticket.util.AmountUtil;
import com.szml.movieticket.vo.SnackOrderItemVO;
import com.szml.movieticket.vo.SnackSelectionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 订单零食选择和库存事务服务。 */
@Service
@RequiredArgsConstructor
public class OrderSnackServiceImpl implements OrderSnackService {

    private static final String RESERVED = "RESERVED";
    private static final String SOLD = "SOLD";
    private static final String RELEASED = "RELEASED";
    private static final int MAX_ITEM_QUANTITY = 10;
    private static final int MAX_TOTAL_QUANTITY = 20;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final ShowtimeMapper showtimeMapper;
    private final HallMapper hallMapper;
    private final CinemaMapper cinemaMapper;
    private final SnackProductMapper snackProductMapper;
    private final OrderSnackItemMapper orderSnackItemMapper;

    @Override
    public SnackSelectionVO getSelection(Long userId, Long orderId) {
        TicketOrder order = requirePendingOrder(userId, orderId, false);
        Cinema cinema = resolveCinema(order);
        List<OrderSnackItem> selectedItems = orderSnackItemMapper.selectList(
                new LambdaQueryWrapper<OrderSnackItem>()
                        .eq(OrderSnackItem::getOrderId, orderId)
                        .eq(OrderSnackItem::getInventoryStatus, RESERVED)
                        .gt(OrderSnackItem::getQuantity, 0)
                        .orderByAsc(OrderSnackItem::getSnackId));
        Map<Long, Integer> selectedQuantities = new HashMap<>();
        for (OrderSnackItem item : selectedItems) {
            selectedQuantities.put(item.getSnackId(), item.getQuantity());
        }

        List<SnackProduct> products = snackProductMapper.selectList(
                new LambdaQueryWrapper<SnackProduct>()
                        .eq(SnackProduct::getCinemaId, cinema.getId())
                        .eq(SnackProduct::getStatus, 1)
                        .orderByAsc(SnackProduct::getCreateTime)
                        .orderByAsc(SnackProduct::getId));
        SnackSelectionVO result = new SnackSelectionVO();
        result.setOrderId(orderId);
        result.setCinemaId(cinema.getId());
        result.setCinemaName(cinema.getName());
        result.setOptions(products.stream().map(product -> toOption(product,
                selectedQuantities.getOrDefault(product.getId(), 0))).toList());
        result.setSelected(selectedItems.stream().map(this::toOrderItemVO).toList());
        result.setTicketAmount(AmountUtil.yuan(getTicketAmountFen(orderId)));
        result.setSnackAmount(AmountUtil.yuan(getReservedSnackAmountFen(orderId)));
        result.setTotalAmount(AmountUtil.yuan(order.getAmount()));
        return result;
    }

    @Override
    @Transactional
    public SnackSelectionVO replaceSelection(Long userId, Long orderId, OrderSnackSelectionDTO dto) {
        TicketOrder order = requirePendingOrder(userId, orderId, true);
        ensurePaymentNotStarted(orderId);
        Cinema cinema = resolveCinema(order);

        Map<Long, Integer> requested = normalizeSelection(dto);
        List<OrderSnackItem> currentItems = orderSnackItemMapper.selectByOrderForUpdate(orderId);
        Map<Long, OrderSnackItem> currentBySnack = new HashMap<>();
        Set<Long> productIds = new HashSet<>(requested.keySet());
        for (OrderSnackItem item : currentItems) {
            currentBySnack.put(item.getSnackId(), item);
            productIds.add(item.getSnackId());
        }

        List<Long> sortedIds = productIds.stream().sorted(Comparator.naturalOrder()).toList();
        Map<Long, SnackProduct> lockedProducts = new LinkedHashMap<>();
        for (Long snackId : sortedIds) {
            SnackProduct product = snackProductMapper.selectForUpdate(snackId);
            if (product == null) {
                throw new OrderException(ErrorCode.SNACK_NOT_FOUND);
            }
            if (!cinema.getId().equals(product.getCinemaId())) {
                throw new OrderException(ErrorCode.SNACK_SELECTION_INVALID);
            }
            int quantity = requested.getOrDefault(snackId, 0);
            if (quantity > 0 && !Integer.valueOf(1).equals(product.getStatus())) {
                throw new OrderException(ErrorCode.SNACK_SELECTION_INVALID);
            }
            lockedProducts.put(snackId, product);
        }

        for (Long snackId : sortedIds) {
            SnackProduct product = lockedProducts.get(snackId);
            OrderSnackItem current = currentBySnack.get(snackId);
            int oldQuantity = current != null && RESERVED.equals(current.getInventoryStatus())
                    ? safeQuantity(current.getQuantity()) : 0;
            int newQuantity = requested.getOrDefault(snackId, 0);
            int delta = newQuantity - oldQuantity;
            if (delta > 0 && safeQuantity(product.getStock()) < delta) {
                throw new OrderException(ErrorCode.SNACK_STOCK_NOT_ENOUGH);
            }
            if (delta != 0) {
                product.setStock(safeQuantity(product.getStock()) - delta);
                snackProductMapper.updateById(product);
            }

            if (current == null) {
                if (newQuantity > 0) {
                    OrderSnackItem item = new OrderSnackItem();
                    item.setOrderId(orderId);
                    item.setSnackId(snackId);
                    item.setSnackName(product.getName());
                    item.setUnitPriceFen(product.getPriceFen());
                    item.setQuantity(newQuantity);
                    item.setInventoryStatus(RESERVED);
                    orderSnackItemMapper.insert(item);
                }
            } else if (newQuantity > 0) {
                current.setSnackName(product.getName());
                current.setUnitPriceFen(product.getPriceFen());
                current.setQuantity(newQuantity);
                current.setInventoryStatus(RESERVED);
                orderSnackItemMapper.updateById(current);
            } else if (RESERVED.equals(current.getInventoryStatus())) {
                // 保留明细快照，只把库存状态改成已释放，便于历史订单追溯。
                current.setInventoryStatus(RELEASED);
                orderSnackItemMapper.updateById(current);
            }
        }

        int ticketAmount = getTicketAmountFen(orderId);
        int snackAmount = getReservedSnackAmountFen(orderId);
        order.setAmount(ticketAmount + snackAmount);
        orderMapper.updateById(order);
        return getSelection(userId, orderId);
    }

    @Override
    public List<SnackOrderItemVO> listOrderItems(Long orderId) {
        List<OrderSnackItem> items = orderSnackItemMapper.selectList(
                new LambdaQueryWrapper<OrderSnackItem>()
                        .eq(OrderSnackItem::getOrderId, orderId)
                        .gt(OrderSnackItem::getQuantity, 0)
                        .orderByAsc(OrderSnackItem::getId));
        return items.stream().map(this::toOrderItemVO).toList();
    }

    @Override
    public int getSnackAmountFen(Long orderId) {
        return orderSnackItemMapper.selectList(new LambdaQueryWrapper<OrderSnackItem>()
                        .eq(OrderSnackItem::getOrderId, orderId)
                        .in(OrderSnackItem::getInventoryStatus, RESERVED, SOLD)
                        .gt(OrderSnackItem::getQuantity, 0))
                .stream()
                .mapToInt(item -> safeQuantity(item.getQuantity()) * safeQuantity(item.getUnitPriceFen()))
                .sum();
    }

    @Override
    @Transactional
    public void markSold(Long orderId) {
        List<OrderSnackItem> items = orderSnackItemMapper.selectByOrderForUpdate(orderId);
        List<OrderSnackItem> reserved = items.stream()
                .filter(item -> RESERVED.equals(item.getInventoryStatus()) && safeQuantity(item.getQuantity()) > 0)
                .toList();
        for (OrderSnackItem item : reserved) {
            SnackProduct product = snackProductMapper.selectForUpdate(item.getSnackId());
            if (product != null) {
                product.setSoldCount(safeQuantity(product.getSoldCount()) + safeQuantity(item.getQuantity()));
                snackProductMapper.updateById(product);
            }
            item.setInventoryStatus(SOLD);
            orderSnackItemMapper.updateById(item);
        }
    }

    @Override
    @Transactional
    public void releaseReserved(Long orderId) {
        List<OrderSnackItem> items = orderSnackItemMapper.selectByOrderForUpdate(orderId);
        List<OrderSnackItem> reserved = items.stream()
                .filter(item -> RESERVED.equals(item.getInventoryStatus()) && safeQuantity(item.getQuantity()) > 0)
                .toList();
        List<Long> productIds = reserved.stream().map(OrderSnackItem::getSnackId).distinct().sorted().toList();
        Map<Long, SnackProduct> products = new HashMap<>();
        for (Long productId : productIds) {
            SnackProduct product = snackProductMapper.selectForUpdate(productId);
            if (product != null) {
                products.put(productId, product);
            }
        }
        for (OrderSnackItem item : reserved) {
            SnackProduct product = products.get(item.getSnackId());
            if (product != null) {
                product.setStock(safeQuantity(product.getStock()) + safeQuantity(item.getQuantity()));
                snackProductMapper.updateById(product);
            }
            item.setInventoryStatus(RELEASED);
            orderSnackItemMapper.updateById(item);
        }
    }

    private TicketOrder requirePendingOrder(Long userId, Long orderId, boolean lock) {
        TicketOrder order = lock ? orderMapper.selectForUpdate(orderId) : orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new OrderException(ErrorCode.ORDER_EXPIRED);
        }
        return order;
    }

    private void ensurePaymentNotStarted(Long orderId) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment != null && "PENDING".equals(payment.getStatus())) {
            throw new OrderException(ErrorCode.SNACK_PAYMENT_LOCKED);
        }
    }

    private Cinema resolveCinema(TicketOrder order) {
        Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
        Hall hall = showtime == null ? null : hallMapper.selectById(showtime.getHallId());
        Cinema cinema = hall == null ? null : cinemaMapper.selectById(hall.getCinemaId());
        if (cinema == null) {
            throw new OrderException(ErrorCode.CINEMA_NOT_FOUND);
        }
        return cinema;
    }

    private Map<Long, Integer> normalizeSelection(OrderSnackSelectionDTO dto) {
        if (dto == null || dto.getItems() == null) {
            throw new OrderException(ErrorCode.SNACK_SELECTION_INVALID);
        }
        Map<Long, Integer> requested = new HashMap<>();
        int total = 0;
        for (OrderSnackItemDTO item : dto.getItems()) {
            if (item == null || item.getSnackId() == null || item.getQuantity() == null
                    || item.getQuantity() < 1 || item.getQuantity() > MAX_ITEM_QUANTITY
                    || requested.put(item.getSnackId(), item.getQuantity()) != null) {
                throw new OrderException(ErrorCode.SNACK_SELECTION_INVALID);
            }
            total += item.getQuantity();
        }
        if (total > MAX_TOTAL_QUANTITY) {
            throw new OrderException(ErrorCode.SNACK_SELECTION_INVALID);
        }
        return requested;
    }

    private int getTicketAmountFen(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId))
                .stream()
                .mapToInt(item -> safeQuantity(item.getUnitPrice()))
                .sum();
    }

    private int getReservedSnackAmountFen(Long orderId) {
        return orderSnackItemMapper.selectList(new LambdaQueryWrapper<OrderSnackItem>()
                        .eq(OrderSnackItem::getOrderId, orderId)
                        .eq(OrderSnackItem::getInventoryStatus, RESERVED)
                        .gt(OrderSnackItem::getQuantity, 0))
                .stream()
                .mapToInt(item -> safeQuantity(item.getQuantity()) * safeQuantity(item.getUnitPriceFen()))
                .sum();
    }

    private SnackSelectionVO.Option toOption(SnackProduct product, int selectedQuantity) {
        SnackSelectionVO.Option option = new SnackSelectionVO.Option();
        option.setId(product.getId());
        option.setName(product.getName());
        option.setDescription(product.getDescription());
        option.setImage(product.getImage());
        option.setPriceFen(product.getPriceFen());
        option.setAvailableStock(safeQuantity(product.getStock()) + selectedQuantity);
        option.setSelectedQuantity(selectedQuantity);
        option.setStatus(product.getStatus());
        return option;
    }

    private SnackOrderItemVO toOrderItemVO(OrderSnackItem item) {
        SnackOrderItemVO vo = new SnackOrderItemVO();
        vo.setSnackId(item.getSnackId());
        vo.setName(item.getSnackName());
        SnackProduct product = snackProductMapper.selectById(item.getSnackId());
        if (product != null) {
            vo.setImage(product.getImage());
        }
        vo.setUnitPrice(AmountUtil.yuan(item.getUnitPriceFen()));
        vo.setQuantity(item.getQuantity());
        vo.setAmount(AmountUtil.yuan(safeQuantity(item.getUnitPriceFen()) * safeQuantity(item.getQuantity())));
        vo.setInventoryStatus(item.getInventoryStatus());
        return vo;
    }

    private static int safeQuantity(Integer value) {
        return value == null ? 0 : value;
    }
}
