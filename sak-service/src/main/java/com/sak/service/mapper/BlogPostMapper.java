package com.sak.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sak.service.entity.BlogPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogPostMapper extends BaseMapper<BlogPost> {
}
