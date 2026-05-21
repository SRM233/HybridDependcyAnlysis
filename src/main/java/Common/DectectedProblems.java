package Common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DectectedProblems {
    public static final Set<String> TARGET_ANNOTATIONS = Set.of(
            // EJB 有状态/无状态/单例
            "jakarta.ejb.Stateful", "javax.ejb.Stateful",
            "jakarta.ejb.Stateless", "javax.ejb.Stateless",
            "jakarta.ejb.Singleton", "javax.ejb.Singleton",

            // 消息驱动 Bean
            "jakarta.ejb.MessageDriven", "javax.ejb.MessageDriven",

            // EJB 远程调用
            "jakarta.ejb.Remote", "javax.ejb.Remote",

            // EJB 本地调用（可保留或移除，视严格程度而定）
            // "jakarta.ejb.Local", "javax.ejb.Local",

            // 容器管理的持久化上下文
            "jakarta.persistence.PersistenceContext", "javax.persistence.PersistenceContext",

            // JNDI 资源注入
            "jakarta.annotation.Resource", "javax.annotation.Resource"
    );

    public static final Map<String, String> SUGGESTIONS_MAP;

    static {
        Map<String, String> map = new HashMap<>();

        // --- Stateful Beans ---
        map.put("jakarta.ejb.Stateful",
                "[Stateful Bean] Not designed for horizontal scaling. State is lost on pod restart. Migrate to stateless JWT + external session store (e.g., Redis).");
        map.put("javax.ejb.Stateful",
                "[Stateful Bean] Not designed for horizontal scaling. State is lost on pod restart. Migrate to stateless JWT + external session store (e.g., Redis).");

        // --- Stateless Beans ---
        map.put("jakarta.ejb.Stateless",
                "[Stateless Bean] Still depends on EJB container lifecycle. Consider refactoring to CDI @ApplicationScoped + @Transactional.");
        map.put("javax.ejb.Stateless",
                "[Stateless Bean] Still depends on EJB container lifecycle. Consider refactoring to CDI @ApplicationScoped + @Transactional.");

        // --- Singleton Beans ---
        map.put("jakarta.ejb.Singleton",
                "[Singleton Bean] Not global across pods. Each replica has its own instance. Use distributed locks or leader election.");
        map.put("javax.ejb.Singleton",
                "[Singleton Bean] Not global across pods. Each replica has its own instance. Use distributed locks or leader election.");

        // --- Message-Driven Beans ---
        map.put("jakarta.ejb.MessageDriven",
                "[Message-Driven Bean] Relies on JMS and JNDI. Replace with cloud-native messaging (Kafka, RabbitMQ, SQS).");
        map.put("javax.ejb.MessageDriven",
                "[Message-Driven Bean] Relies on JMS and JNDI. Replace with cloud-native messaging (Kafka, RabbitMQ, SQS).");

        // --- Remote EJB ---
        map.put("jakarta.ejb.Remote",
                "[Remote Interface] Uses RMI-IIOP, which is not compatible with standard K8s Ingress/Services. Refactor to REST or gRPC.");
        map.put("javax.ejb.Remote",
                "[Remote Interface] Uses RMI-IIOP, which is not compatible with standard K8s Ingress/Services. Refactor to REST or gRPC.");

        // --- Local EJB (optional) ---
        map.put("jakarta.ejb.Local",
                "[Local Interface] Requires EJB container. Migrate business logic to CDI beans.");
        map.put("javax.ejb.Local",
                "[Local Interface] Requires EJB container. Migrate business logic to CDI beans.");

        // --- Container-Managed Persistence Context ---
        map.put("jakarta.persistence.PersistenceContext",
                "[Persistence Context] Container-managed EntityManager may behave differently in lightweight containers. Verify transaction boundaries.");
        map.put("javax.persistence.PersistenceContext",
                "[Persistence Context] Container-managed EntityManager may behave differently in lightweight containers. Verify transaction boundaries.");

        // --- JNDI Resource Injection ---
        map.put("jakarta.annotation.Resource",
                "[JNDI Resource] Causes startup failures in cloud-native runtimes due to missing JNDI. Use environment variables or @Value instead.");
        map.put("javax.annotation.Resource",
                "[JNDI Resource] Causes startup failures in cloud-native runtimes due to missing JNDI. Use environment variables or @Value instead.");

        SUGGESTIONS_MAP = Collections.unmodifiableMap(map);
    }
}

