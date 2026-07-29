package com.limou.movieticket.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.movieticket.auth.domain.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
    @Select("SELECT * FROM refresh_token WHERE token_hash = #{tokenHash} FOR UPDATE")
    RefreshToken selectByHashForUpdate(@Param("tokenHash") String tokenHash);
}
