package Common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result <T>{
    private Integer code;
    private String message;
    private T data;

    public static<T> Result<T> succes(T data)
    {
        return new Result<>(200, "Success", data);
    }

    public static<T> Result<T> success()
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage("Success");


        return result;
    }

    public static<T> Result<T> success(T data)
    {
        Result result = new Result<>();
        result.setCode(200);
        result.setMessage("Success");
        result.setData(data);
        return result;
    }

    public static Result fail(String s) {
        Result result = new Result<>();
        result.setCode(400);
        result.setMessage(s);
        return result;
    }
}
