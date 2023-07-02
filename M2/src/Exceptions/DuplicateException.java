package Exceptions;
public class DuplicateException extends DBAppException {
  
    public DuplicateException (){
        super();
    }

    public DuplicateException(String s){
        super(s);
    }
}
