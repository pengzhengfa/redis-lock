package com.redisson;

import org.redisson.Redisson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 利用java锁来模拟扣库存
 *
 * @author pengzhengfa
 */
@RestController
public class IndexController {

    @Autowired
    private Redisson redisson;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 我们来这样子写分布式锁有什么问题呢,万一我执行到某一行代码抛异常了呢
     *
     * @return
     */
    @RequestMapping("/deduct_stock")
    public String deductStock() {
        try {
            String lockKey = "lockKey";
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent("lockKey", "zhuge");
            if (!result) {
                return "1001";
            }
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", realStock + "");
                System.out.println("扣减库存成功,剩余库存:" + realStock + "");
            } else {
                System.out.println("扣减库存失败,库存不足");
            }

        }finally {
            stringRedisTemplate.delete("lockKey");
        }
        return "end";
    }
}
