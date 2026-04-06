package com.superkiller.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.superkiller.backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
