package com.mongodb.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Encrypted;
import org.springframework.data.mongodb.core.mapping.Queryable;
import org.springframework.data.mongodb.core.mapping.RangeEncrypted;

@Document(collection = "employees")
public record Employee(
        @Id
        String id,

        String name,

        // - @Encrypted : 这是一个基本的加密注解。它告诉 Spring Data：“在将这个 pin 字段存入数据库之前，请对它进行加密。”
        // - 功能 : 这种加密方式被称为 确定性加密 （Deterministic Encryption）。它的主要作用是保护数据，确保数据库中的 pin 字段是以密文形式存储的。
        // - 限制 : 仅使用 @Encrypted 注解的字段是 不可查询 的。也就是说，您无法执行像 findByPin("1234") 这样的查询来找到某个员工，因为加密后的值是无法直接匹配的。这个注解适用于那些只需要安全存储、不需要作为查询条件的数据。
        @Encrypted
        String pin,

        // - @Encrypted : 和 pin 字段一样， ssn 字段在存入数据库时也会被加密。
        // - @Queryable(queryType = "equality") : 这是关键所在。 @Queryable 注解让加密后的字段变得 可以查询 。
        // - queryType = "equality" : 这个参数指定了查询类型为 等值查询 。这意味着，即使 ssn 在数据库中是加密的，您仍然可以执行精确匹配查询，例如 findBySsn(123456789) 。MongoDB 的加密库会以一种特殊的方式处理查询，使其能够在不解密整个集合的情况下找到匹配的加密数据。
        @Queryable(queryType = "equality")
        @Encrypted
        int ssn,

        // - @RangeEncrypted : 这是一个更高级的加密注解，专门用于需要进行 范围查询 （例如，大于、小于）的数值或日期字段。
        // - 功能 : 它同样会对 age 字段进行加密，但使用的加密算法支持范围比较。这样，您就可以执行像 findByAgeLessThan(40) 或 findByAgeGreaterThan(30) 这样的查询。
        // - 参数解释 :
        // - rangeOptions = "{\"min\": 0, \"max\": 150}" : 这里定义了 age 字段的数值范围。您需要为范围查询的字段提供一个预估的最小值和最大值。这个信息能帮助加密算法优化存储和查询性能。设置一个精确的范围非常重要。
        // - contentionFactor = 0L : 这是一个性能调优参数，用于处理加密数据中可能出现的“冲突”。对于初学者来说，可以暂时将其理解为一个默认配置，通常使用 0 即可。
        @RangeEncrypted(
                contentionFactor = 0L,
                rangeOptions = "{\"min\": 0, \"max\": 150}"
        )
        Integer age,

        @RangeEncrypted(contentionFactor = 0L,
                rangeOptions = "{\"min\": {\"$numberDouble\": \"1500\"}, \"max\": {\"$numberDouble\": \"100000\"}, \"precision\": 2 }")
        double salary
) {}
