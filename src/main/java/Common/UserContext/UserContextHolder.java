package Common.UserContext;

import com.hybriddependcyanlysis.POJO.DTO.UserLoginDTO;

public class UserContextHolder {

// ThreadLocal
// https://jishuzhan.net/article/2001538049470169089#%E5%AE%9E%E6%88%98%EF%BC%9A%E7%94%A8%E6%88%B7%E4%B8%8A%E4%B8%8B%E6%96%87%E4%BC%A0%E9%80%92

    // Define ThreadLocal instance
    private static final ThreadLocal<UserLoginDTO> USER_THREAD_LOCAL = ThreadLocal.withInitial(() -> null);


    private UserContextHolder() {}


    public static void setUser(UserLoginDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    public static UserLoginDTO getUser() {
        return USER_THREAD_LOCAL.get();
    }

    public static Integer getUserId() {
        UserLoginDTO user = getUser();
        return user == null ? null : user.getUser().getId();
    }

    public static String getUsername() {
        UserLoginDTO user = getUser();
        return user == null ? null : user.getUser().getUsername();
    }


    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }

}
