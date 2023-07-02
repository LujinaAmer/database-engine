import java.io.Serializable;

public enum OctLocations implements Serializable{
    TopLeftFront(0),
    TopRightFront(1),
    BottomRightFront(2),
    BottomLeftFront(3),
    TopLeftBack(4),
    TopRightBack(5),
    BottomRightBack(6),
    BottomLeftBack(7);

    int num;

    OctLocations(int num){
        this.num = num;
    }

    int getNumber(){
        return num;
    }
}
