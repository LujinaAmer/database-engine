package Exceptions;
public class NotFoundException extends DBAppException {
  
    public NotFoundException (){
        super();
    }

    public NotFoundException(String s){
        super(s);
    }
}