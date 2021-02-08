package com.bharatpe.cache.controller;

//@RestController
//@RequestMapping("cache")
public class cacheController {

//    private final Logger logger = LoggerFactory.getLogger(cacheController.class);
//
//    @Autowired
//    RedisTemplate<String, Object> redisTemplate;
//
//    @PostMapping(value = "/add")
//    public Object add(@RequestBody RequestDto requestDto){
//        if(requestDto.validate()){
//            String key = requestDto.getKey();
//            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
//            final boolean hasKey = redisTemplate.hasKey(requestDto.getKey());
//
//            operations.set(key, requestDto.getValue(), 24, TimeUnit.HOURS);
//            logger.info(": cache insert key:{} >> value : {} ", key, requestDto.getValue());
//
//            return true;
//        }
//        return false;
//    }
//
//    @GetMapping(value = "/get")
//    public Object get(@RequestParam String key){
//
//        if(Objects.nonNull(key)){
//            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
//            final boolean hasKey = redisTemplate.hasKey(key);
//
//            if (hasKey) {
//                logger.info(": fetched from cache key:{}  ", key);
//                return operations.get(key);
//            }
//        }
//
//        return false;
//    }
}
