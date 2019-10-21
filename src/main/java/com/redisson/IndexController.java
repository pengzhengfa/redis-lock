package com.redisson;

import org.redisson.Redisson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
     * 也有可能出现当我执行到某一段代码之间,我们的服务器发生宕机呢,那么我的key
     * 会长久的存在于redis当中,也有可能我这个线程的key,被另外一个线程删除(锁失效)
     * 那么这个问题该怎么解决呢
     *
     * @return
     */
    @RequestMapping("/deduct_stock")
    public String deductStock() {
        String lockKey = "product_001";
        String clientId = UUID.randomUUID().toString();
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, clientId,10,TimeUnit.SECONDS);
            //设置超时时间
            stringRedisTemplate.expire(lockKey,30,TimeUnit.SECONDS);
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
            //谁加的锁,谁去释放
            if (clientId.equals(stringRedisTemplate.opsForValue().get(lockKey))){
                stringRedisTemplate.delete("lockKey");
            }
        }
        return "end";
    }
}
