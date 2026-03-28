package Common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//封装返回类Result，参考链接：https://developer.aliyun.com/article/1248568

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result <T>{
    private Integer code;
    private String message;
    private T data;

//    public static<T> Result<T> succes(T data)
//    {
//        return new Result<>(200, "Success", data);
//    }

    //只返回成功信息
    public static<T> Result<T> success()
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage("Success");


        return result;
    }


    //返回成功信息和数据
    public static<T> Result<T> success(T data)
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage("Success");
        result.setData(data);
        return result;
    }

    //返回输入的message和数据
    public static<T> Result<T> success(String message, T data)
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    //返回失败信息
    public static Result fail(String s) {
        Result result = new Result<>();
        result.setCode(400);
        result.setMessage(s);
        return result;
    }
}
