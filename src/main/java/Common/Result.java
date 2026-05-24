package Common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


// Encapsulated return class Result, reference: https://developer.aliyun.com/article/1248568

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

    // Return success message only
    public static<T> Result<T> success()
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage("Success");


        return result;
    }


    // Return success message and data
    public static<T> Result<T> success(T data)
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage("Success");
        result.setData(data);
        return result;
    }

    // Return input message and data
    public static<T> Result<T> success(String message, T data)
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // Return failure message
    public static Result fail(String s) {
        Result result = new Result<>();
        result.setCode(400);
        result.setMessage(s);
        return result;
    }
}
