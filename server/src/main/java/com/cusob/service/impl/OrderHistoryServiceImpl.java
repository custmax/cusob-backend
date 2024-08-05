package com.cusob.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cusob.auth.AuthContext;
import com.cusob.entity.OrderHistory;
import com.cusob.entity.User;
import com.cusob.mapper.OrderHistoryMapper;
import com.cusob.service.OrderHistoryService;
import com.cusob.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderHistoryServiceImpl
        extends ServiceImpl<OrderHistoryMapper, OrderHistory>
        implements OrderHistoryService {

    @Autowired
    private UserService userService;

    /**
     * save Order History
     * @param orderHistory
     */
    @Override
    public void saveOrderHistory(OrderHistory orderHistory) {
        baseMapper.insert(orderHistory);
    }

    /**
     * get Order History Page
     * @param pageParam
     * @return
     */
    @Override
    public IPage<OrderHistory> getOrderHistoryPage(Page<OrderHistory> pageParam) {
        Long userId = AuthContext.getUserId();
        Long companyId = AuthContext.getCompanyId();
        User user = userService.getById(userId);
        IPage<OrderHistory> page;
        if (user.getPermission().equals(User.USER)){
            // user
            page = baseMapper.selectPage(pageParam,
                    new LambdaQueryWrapper<OrderHistory>()
                            .eq(OrderHistory::getUserId, userId)
            );
        }else {
            // admin
            page = baseMapper.selectPage(pageParam,
                    new LambdaQueryWrapper<OrderHistory>()
                            .eq(OrderHistory::getCompanyId, companyId)
            );
        }
        return page;
    }
}
