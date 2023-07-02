import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import Exceptions.DBAppException;
import Exceptions.OutOfBoundsException;

public class Octree implements Serializable{

	private OctPoint point;

	private OctPoint topLeftFront, bottomRightBack;

	private Octree[] children = new Octree[8];

	private Vector<Object[]> refrences; // [ x , y , z  , < [ pk ,pageID ] [ pk1 ,pageID1 ] [...]> ] , [x , y , z  , < [ pk pageID tableName ] [duplicate] > ]            
	private int capacity;

	public Octree() {
		point = new OctPoint();
	}

	public Octree(Object x, Object y, Object z, Object[] object) {
		point = new OctPoint(x, y, z);
		Properties prop = new Properties();
		String fileName = "src/resources/DBApp.config";
		try (FileInputStream fis = new FileInputStream(fileName)) {
			prop.load(fis);
		} catch (IOException ex) {
			System.out.println("Config not found");
		}

		capacity = Integer.parseInt(prop.getProperty("MaximumEntriesinOctreeNode"));
		this.refrences = new Vector<>(capacity);
		this.refrences.add(object);
	}

	public Octree(Object x1, Object y1, Object z1, Object x2, Object y2, Object z2) throws OutOfBoundsException {
		if ( comparing(x2, x1) <= 0 || comparing(y2, y1) <= 0 || comparing(z2, z1) <= 0) {
			throw new OutOfBoundsException("The bounds are not properly set!");
		}

		point = null;
		topLeftFront = new OctPoint(x1, y1, z1);
		bottomRightBack = new OctPoint(x2, y2, z2);

		for (int i = 0; i <= 7; i++) {
			children[i] = new Octree();
		}
	}

	public void insert(Object x, Object y, Object z, Object[] records) throws OutOfBoundsException {
		if ( find(x, y, z , records)) {
			// throw new DBAppException("Point already exists in the tree. X: " + x + " Y: "
			// + y + " Z: " + z
			// + " Object Name: " + object.getClass().getName());
			return ;
		}

		if (comparing(x, topLeftFront.getX()) < 0 || comparing(x, bottomRightBack.getX()) > 0
				|| comparing(y, topLeftFront.getY()) < 0 || comparing(y, bottomRightBack.getY()) > 0
				|| comparing(z, topLeftFront.getZ()) < 0 || comparing(z, bottomRightBack.getZ()) > 0) {
			throw new OutOfBoundsException("Insertion point is out of bounds! X: " + x + " Y: " + y + " Z: " + z
					+ " Object Name: " + records.getClass().getName());
		}

		Object midx = findMid(topLeftFront.getX(), bottomRightBack.getX());
		Object midy = findMid(topLeftFront.getY(), bottomRightBack.getY());
		Object midz = findMid(topLeftFront.getZ(), bottomRightBack.getZ());

		int pos = findPosition(x, y, z, midx, midy, midz);

		if (children[pos].point == null) {
			children[pos].insert(x, y, z, records);
		} else if (children[pos].point.isNullified()) {
			children[pos] = new Octree(x, y, z, records);
		} else {
			//			Object x_ = children[pos].point.getX();
			//			Object y_ = children[pos].point.getY();
			//			Object z_ = children[pos].point.getZ();

			//T object_ = children[pos].object;
			Vector<Object[]> temp = new Vector<>();

			if (children[pos].refrences.size() < capacity) {
				children[pos].refrences.add(records) ;

				return ;
			}else {

				for (Object[] obj : children[pos].refrences ) {
					temp.add(obj);
				}

			}

			children[pos] = null;
			if (pos == OctLocations.TopLeftFront.getNumber()) {
				children[pos] = new Octree(topLeftFront.getX(), topLeftFront.getY(), topLeftFront.getZ(), midx, midy,
						midz);
			} else if (pos == OctLocations.TopRightFront.getNumber()) {
				children[pos] = new Octree((midx), topLeftFront.getY(), topLeftFront.getZ(),
						bottomRightBack.getX(),
						midy, midz);
			} else if (pos == OctLocations.BottomRightFront.getNumber()) {
				children[pos] = new Octree((midx), (midy), topLeftFront.getZ(), bottomRightBack.getX(),
						bottomRightBack.getY(), midz);
			} else if (pos == OctLocations.BottomLeftFront.getNumber()) {
				children[pos] = new Octree(topLeftFront.getX(), (midy), topLeftFront.getZ(), midx,
						bottomRightBack.getY(), midz);
			} else if (pos == OctLocations.TopLeftBack.getNumber()) {
				children[pos] = new Octree(topLeftFront.getX(), topLeftFront.getY(), (midz), midx, midy,
						bottomRightBack.getZ());
			} else if (pos == OctLocations.TopRightBack.getNumber()) {
				children[pos] = new Octree((midx), topLeftFront.getY(), (midz), bottomRightBack.getX(),
						midy,
						bottomRightBack.getZ());
			} else if (pos == OctLocations.BottomRightBack.getNumber()) {
				children[pos] = new Octree((midx), (midy), (midz), bottomRightBack.getX(),
						bottomRightBack.getY(), bottomRightBack.getZ());
			} else if (pos == OctLocations.BottomLeftBack.getNumber()) {
				children[pos] = new Octree(topLeftFront.getX(), (midy), (midz), midx,
						bottomRightBack.getY(),
						bottomRightBack.getZ());
			}

			//children[pos].insert(x_, y_, z_, object_);

			for (int i = 0 ; i < temp.size() ;  i++) {
				children[pos].insert(temp.get(i)[0] , temp.get(i)[1], temp.get(i)[2], temp.get(i));
			}
			children[pos].insert(x, y, z, records);
		}

	}

	// private Object plusOne(Object mid) {

	// 	Object result = null;
	// 	if (mid.getClass().getName().equals("java.lang.Integer")) {
	// 		result = (java.lang.Integer) mid + 1;
	// 		return result;
	// 	}

	// 	if (mid.getClass().getName().equals("java.lang.Double")) {
	// 		result = (java.lang.Double) mid + 0.01;
	// 		return result;
	// 	}

	// 	if (mid.getClass().getName().equals("java.util.Date")) {

	// 		LocalDate temp = ((Date) mid).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	// 		LocalDate added = temp.plusDays(1);
	// 		result = Date.from(added.atStartOfDay(ZoneId.systemDefault()).toInstant());

	// 		return result;
	// 	}

	// 	if (mid.getClass().getName().equals("java.lang.String")) {
	// 		char temp = (char) (((String) mid).charAt(((String) mid).length() - 1) + 1);
	// 		result = ((String) mid).substring(0, ((String) mid).length() - 1) + temp;
	// 		return result;
	// 	}
	// 	return result;

	// }

	private int findPosition(Object x, Object y, Object z, Object midx, Object midy, Object midz) {
		int pos;

		if (comparing(x, midx) < 0) {
			if (comparing(y, midy) < 0) {
				if (comparing(z, midz) < 0)
					pos = OctLocations.TopLeftFront.getNumber();
				else
					pos = OctLocations.TopLeftBack.getNumber();
			} else {
				if (comparing(z, midz) < 0)
					pos = OctLocations.BottomLeftFront.getNumber();
				else
					pos = OctLocations.BottomLeftBack.getNumber();
			}
		} else {
			if (comparing(y, midy) < 0) {
				if (comparing(z, midz) < 0)
					pos = OctLocations.TopRightFront.getNumber();
				else
					pos = OctLocations.TopRightBack.getNumber();
			} else {
				if (comparing(z, midz) < 0)
					pos = OctLocations.BottomRightFront.getNumber();
				else
					pos = OctLocations.BottomRightBack.getNumber();
			}
		}
		return pos;
	}

	public boolean find(Object x, Object y, Object z , Object[] records) {
		if (comparing(x, topLeftFront.getX()) < 0 || comparing(x, bottomRightBack.getX()) > 0
				|| comparing(y, topLeftFront.getY()) < 0 || comparing(y, bottomRightBack.getY()) > 0
				|| comparing(z, topLeftFront.getZ()) < 0 || comparing(z, bottomRightBack.getZ()) > 0)
			return false;


		Object midx = findMid(topLeftFront.getX(), bottomRightBack.getX());
		Object midy = findMid(topLeftFront.getY(), bottomRightBack.getY());
		Object midz = findMid(topLeftFront.getZ(), bottomRightBack.getZ());

		int pos = findPosition(x, y, z, midx, midy, midz);

		if (children[pos].point == null) 
			return children[pos].find(x, y, z , records);
		if (children[pos].point.isNullified())
			return false; 

		//return x ==  && y == children[pos].point.getY() && z == children[pos].point.getZ();

		for (int i = 0 ;  i < children[pos].refrences.size() ; i++ ) {
			if ( comparing(x, children[pos].refrences.get(i)[0] ) ==0 
					&& comparing(y , children[pos].refrences.get(i)[1] ) ==0
					&& comparing(z , children[pos].refrences.get(i)[2] ) ==0 ) {
				((Vector<Object[]>)children[pos].refrences.get(i)[3]).add(( (Vector<Object[]>)records[3]).get(0) );
				return true ;
			}
		}
		return false ;

	}



	public Object[] get(Object x, Object y, Object z) {
		if (comparing(x, topLeftFront.getX()) < 0 || comparing(x, bottomRightBack.getX()) > 0
				|| comparing(y, topLeftFront.getY()) < 0 || comparing(y, bottomRightBack.getY()) > 0
				|| comparing(z, topLeftFront.getZ()) < 0 || comparing(z, bottomRightBack.getZ()) > 0)
			return null;

		Object midx = findMid(topLeftFront.getX(), bottomRightBack.getX());
		Object midy = findMid(topLeftFront.getY(), bottomRightBack.getY());
		Object midz = findMid(topLeftFront.getZ(), bottomRightBack.getZ());

		int pos = findPosition(x, y, z, midx, midy, midz);

		if (children[pos].point == null)
			return children[pos].get(x, y, z);
		if (children[pos].point.isNullified())
			return null;

		for (int i =0 ; i < children[pos].refrences.size() ; i ++) {
			if (comparing(x , children[pos].refrences.get(i)[0]) == 0 
					&& comparing( y , children[pos].refrences.get(i)[1]) == 0
					&& comparing(z , children[pos].refrences.get(i)[2]) == 0) {
				return children[pos].refrences.get(i);
			}
		}

		return null;
	}

	public Object[] update(Object x, Object y, Object z , Object pk) {
		if (comparing(x, topLeftFront.getX()) < 0 || comparing(x, bottomRightBack.getX()) > 0
				|| comparing(y, topLeftFront.getY()) < 0 || comparing(y, bottomRightBack.getY()) > 0
				|| comparing(z, topLeftFront.getZ()) < 0 || comparing(z, bottomRightBack.getZ()) > 0)
			return null;

		Object midx = findMid(topLeftFront.getX(), bottomRightBack.getX());
		Object midy = findMid(topLeftFront.getY(), bottomRightBack.getY());
		Object midz = findMid(topLeftFront.getZ(), bottomRightBack.getZ());

		//		int [] oldPos = {0,1,2,3,4,5,6,7};
		int [] newPosX = {0,1,2,3,4,5,6,7};
		int [] newPosY = {0,1,2,3,4,5,6,7};
		int [] newPosZ = {0,1,2,3,4,5,6,7};

		if (x != null) {
			if (comparing(x, midx ) < 0 ) {
				newPosX[1] = -1;
				newPosX[2] = -1;
				newPosX[5] = -1;
				newPosX[6] = -1;
			}
			else {
				newPosX[0] = -1;
				newPosX[3] = -1;
				newPosX[4] = -1;
				newPosX[7] = -1;
			}

		}
		if(y != null) {
			if (comparing(y, midy ) < 0 ) {
				newPosY[2] = -1;
				newPosY[3] = -1;
				newPosY[6] = -1;
				newPosY[7] = -1;
			}
			else {
				newPosY[0] = -1;
				newPosY[1] = -1;
				newPosY[4] = -1;
				newPosY[5] = -1;
			}
		}
		if(z != null) {
			if (comparing(z, midz ) < 0 ) {
				newPosZ[4] = -1;
				newPosZ[5] = -1;
				newPosZ[6] = -1;
				newPosZ[7] = -1;
			}
			else {
				newPosZ[0] = -1;
				newPosZ[1] = -1;
				newPosZ[2] = -1;
				newPosZ[3] = -1;
			}
		}
		Vector<Integer> newPos = new Vector<>();
		Object[] result  = null;
		for(int i = 0 ; i < 8 ;  i++) {
			if(newPosX[i] != -1 && newPosY[i] != -1 && newPosZ[i] != -1) {
				newPos.add(i);
			}
		}

		//int pos = findPosition(x, y, z, midx, midy, midz);
		for (int i =0 ; i < newPos.size() ; i ++) {
			int pos = newPos.get(i);

			if (children[pos].point == null) {
				result = children[pos].update(x, y, z, pk) ;
				continue ;
			}

			if (children[pos].point.isNullified()) {
				newPos.remove(pos);
				i--;
				continue ;
			}

			for (int j =0 ; j < children[pos].refrences.size() ; j ++) {

				if (((x == null ) || comparing(x , children[pos].refrences.get(j)[0]) == 0) &&
						((y == null ) || comparing( y , children[pos].refrences.get(j)[1]) == 0) &&
						((z == null ) || comparing(z , children[pos].refrences.get(j)[2]) == 0) 
						) { 
					for (int k= 0 ; k < ((Vector<Object[]>)children[pos].refrences.get(j)[3]).size() ; k ++) {

						if(comparing( pk , ((Vector<Object[]>)children[pos].refrences.get(j)[3]).get(k)[0] )  == 0) {

							result = ((Vector<Object[]>)children[pos].refrences.get(j)[3]).get(k) ;
							((Vector<Object[]>)children[pos].refrences.get(j)[3]).remove(k) ;

							if(((Vector<Object[]>)children[pos].refrences.get(j)[3]).isEmpty()) {
								children[pos].refrences.remove(j);
							}
							return result ;
						}
					}


				}
			}
		}
		return result;
	}


	public Vector<Object[]> remove(Object x, Object y, Object z) {
		if (comparing(x, topLeftFront.getX()) < 0 || comparing(x, bottomRightBack.getX()) > 0
				|| comparing(y, topLeftFront.getY()) < 0 || comparing(y, bottomRightBack.getY()) > 0
				|| comparing(z, topLeftFront.getZ()) < 0 || comparing(z, bottomRightBack.getZ()) > 0)
			return null;

		Object midx = findMid(topLeftFront.getX(), bottomRightBack.getX());
		Object midy = findMid(topLeftFront.getY(), bottomRightBack.getY());
		Object midz = findMid(topLeftFront.getZ(), bottomRightBack.getZ());

		//		int [] oldPos = {0,1,2,3,4,5,6,7};
		int [] newPosX = {0,1,2,3,4,5,6,7};
		int [] newPosY = {0,1,2,3,4,5,6,7};
		int [] newPosZ = {0,1,2,3,4,5,6,7};

		if (x != null) {
			if (comparing(x, midx ) < 0 ) {
				newPosX[1] = -1;
				newPosX[2] = -1;
				newPosX[5] = -1;
				newPosX[6] = -1;
			}
			else {
				newPosX[0] = -1;
				newPosX[3] = -1;
				newPosX[4] = -1;
				newPosX[7] = -1;
			}

		}
		if(y != null) {
			if (comparing(y, midy ) < 0 ) {
				newPosY[2] = -1;
				newPosY[3] = -1;
				newPosY[6] = -1;
				newPosY[7] = -1;
			}
			else {
				newPosY[0] = -1;
				newPosY[1] = -1;
				newPosY[4] = -1;
				newPosY[5] = -1;
			}
		}
		if(z != null) {
			if (comparing(z, midz ) < 0 ) {
				newPosZ[4] = -1;
				newPosZ[5] = -1;
				newPosZ[6] = -1;
				newPosZ[7] = -1;
			}
			else {
				newPosZ[0] = -1;
				newPosZ[1] = -1;
				newPosZ[2] = -1;
				newPosZ[3] = -1;
			}
		}
		Vector<Integer> newPos = new Vector<>();
		Vector<Object[]>result = new Vector<>();
		for(int i = 0 ; i < 8 ;  i++) {
			if(newPosX[i] != -1 && newPosY[i] != -1 && newPosZ[i] != -1) {
				newPos.add(i);
			}
		}

		//int pos = findPosition(x, y, z, midx, midy, midz);
		for (int i =0 ; i < newPos.size() ; i ++) {
			int pos = newPos.get(i);

			if (children[pos].point == null) {
				for (Object[] objects : children[pos].remove(x, y, z)) {
					result.add(objects);
				}
				continue ;
			}

			if (children[pos].point.isNullified()) {
				newPos.remove(pos);
				i--;
				continue ;
			}

			for (int j =0 ; j < children[pos].refrences.size() ; j ++) {

				if (((x == null ) || comparing(x , children[pos].refrences.get(j)[0]) == 0) &&
						((y == null ) || comparing( y , children[pos].refrences.get(j)[1]) == 0) &&
						((z == null ) || comparing(z , children[pos].refrences.get(j)[2]) == 0)) {
					result.add(children[pos].refrences.get(j));
					children[pos].refrences.remove(j);
					if( children[pos].refrences.isEmpty())
						children[pos].point.setNullify(true);
					//return true ;
				}
			}
		}
		
		return result;
	}



	//	public void print(Octree o) {
	//
	//		for (int i = 0; i <= 7; i++) {
	//			if (null == o.children[i].refrences) {
	//				for (int j = 0; j <= 7; j++) {
	//					System.out.println(o.children[i].children[j].refrences);
	//				}
	//
	//			} else {
	//				System.out.println(o.children[i].refrences);
	//			}
	//		}
	//
	//	}

	private Double comparing(Object value, Object compared) {
		Double difference = 0.0;
		if (value.toString().equals("Null") || compared.toString().equals("Null") 
				||  value ==null  || compared == null) {
			return null;
		}
		if (value.getClass().getName().equals("java.lang.Integer")) {
			difference += (java.lang.Integer) value - ((java.lang.Integer) compared). intValue();
			return difference;
		}

		if (value.getClass().getName().equals("java.lang.Double")) {
			difference = (java.lang.Double) value - ((java.lang.Double) compared).doubleValue();
			return difference;
		}

		if (value.getClass().getName().equals("java.util.Date")) {
			difference += ((java.util.Date) value).compareTo((java.util.Date) compared);
			return difference;
		}

		if (value.getClass().getName().equals("java.lang.String")) {
			difference += ((java.lang.String) value).compareTo(((java.lang.String) compared));
			return difference;
		}
		return difference;
	}

	private Object findMid(Object max, Object min) {
		Object mid = null;
		if (max.getClass().getName().equals("java.lang.Integer")) {
			mid = ((java.lang.Integer) max + (java.lang.Integer) min) / 2;
			return mid;
		}

		if (max.getClass().getName().equals("java.lang.Double")) {
			mid = ((java.lang.Double) max + (java.lang.Double) min) / 2;
			return mid;
		}

		if (max.getClass().getName().equals("java.util.Date")) {

			LocalDate minDate = ((Date) min).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate maxDate = ((Date) max).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate midLocal = minDate.plusDays(ChronoUnit.DAYS.between(minDate, maxDate) / 2);
			mid = Date.from(midLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
			return mid;
		}

		if (max.getClass().getName().equals("java.lang.String")) {
			mid = MiddleString(max.toString(), min.toString());
			return mid;
		}
		return mid;
	}

	private String midString(String min , String max ) {

		int n = 0 ;
		for (int i =0 ; i < min.length() ; i ++ ) {
			n+= min.charAt(i) ;
		}

		for (int i =0 ; i < max.length() ; i ++ ) {
			n+= max.charAt(i) ;
		}

		n = n/2 ;
		return String.valueOf(n);
	}

	private String MiddleString(String min, String max) {
		String result = "";
		String S = min;
		String T = max;
		// Stores the base 26 digits after addition
		int N;
		if (S.length() > T.length()) {
			N = S.length();
			T += S.substring(T.length(), S.length());
		} else {
			N = T.length();
			S += T.substring(S.length(), T.length());
		}

		int[] a1 = new int[N + 1];

		for (int i = 0; i < N; i++) {
			a1[i + 1] = (int) S.charAt(i) - 97
					+ (int) T.charAt(i) - 97;
		}

		// Iterate from right to left
		// and add carry to next position
		for (int i = N; i >= 1; i--) {
			a1[i - 1] += (int) a1[i] / 26;
			a1[i] %= 26;
		}

		// Reduce the number to find the middle
		// string by dividing each position by 2
		for (int i = 0; i <= N; i++) {

			// If current value is odd,
			// carry 26 to the next index value
			if ((a1[i] & 1) != 0) {

				if (i + 1 <= N) {
					a1[i + 1] += 26;
				}
			}

			a1[i] = (int) a1[i] / 2;
		}

		for (int i = 1; i <= N; i++) {
			result += ((char) (a1[i] + 97));
		}

		if (result.equals(S) && result.equals(T)) {

			char temp = (char) (((String) result).charAt(((String) result).length() - 1) - 1);
			result = ((String) result).substring(0, ((String) result).length() - 1) + temp;

		}

		return result;

	}

	

	public Vector<Object[]> select(Object[]x , Object[]y , Object[]z) {
		Vector<Object[]>result = new Vector<>();
		if(x[1].equals("=") || y[1].equals("=") || z[1].equals("=")){
			result = selectequal(x[0],y[0],z[0],(String)x[1],(String)y[1],(String)z[1]);
		}
		else {
			int[] newPosX = getposselectx(x[0],(String)x[1]);
			int[] newPosY = getposselecty(y[0],(String)y[1]);
			int[] newPosZ = getposselectz(z[0],(String)z[1]);

			Vector<Integer> newPos = new Vector<>();
			for(int i = 0 ; i < 8 ;  i++) {
				if(newPosX[i] != -1 && newPosY[i] != -1 && newPosZ[i] != -1) {
					newPos.add(i);
				}
			}

			for (int i =0 ; i < newPos.size() ; i ++) {
				int pos = newPos.get(i);

				if (children[pos].point == null) {
					for (Object[] objects : children[pos].select(x, y, z)) {
						result.add(objects);
					}
					continue ;
				}

				if (children[pos].point.isNullified()) {
					continue ;
				}

				for (int j =0 ; j < children[pos].refrences.size() ; j ++) {

					if (checkchild(children[pos].refrences.get(j)[0] , x[0] ,(String)x[1])  &&
							checkchild(children[pos].refrences.get(j)[1],y[0],(String)y[1]) &&
							checkchild(children[pos].refrences.get(j)[2],z[0],(String)z[1])) {
						result.add(children[pos].refrences.get(j));
					}
				}
			}
		}
		return result;
	}


	private int[] getposselectx(Object x, String operation){
		int[] newPosX = {0,1,2,3,4,5,6,7};
		Object midx = findMid(topLeftFront.getX(), bottomRightBack.getX());
		if(operation.equals(">") || operation.equals(">=")) {
			if(comparing(x,midx) >= 0) {
				newPosX[0] = -1;
				newPosX[3] = -1;
				newPosX[4] = -1;
				newPosX[7] = -1;
			}
		}
		if(operation.equals("<") || operation.equals("<=")) {
			if(comparing(x,midx) <= 0) {
				newPosX[1] = -1;
				newPosX[2] = -1;
				newPosX[5] = -1;
				newPosX[6] = -1;
			}
		}
		return newPosX;
	}
	private int[] getposselecty(Object y, String operation){
		int[] newPosY = {0,1,2,3,4,5,6,7};
		Object midy = findMid(topLeftFront.getY(), bottomRightBack.getY());
		if(operation.equals(">") || operation.equals(">=")) {
			if(comparing(y,midy) >= 0) {
				newPosY[0] = -1;
				newPosY[1] = -1;
				newPosY[4] = -1;
				newPosY[5] = -1;
			}
		}
		if(operation.equals("<") || operation.equals("<=")) {
			if(comparing(y,midy) <= 0) {
				newPosY[2] = -1;
				newPosY[3] = -1;
				newPosY[6] = -1;
				newPosY[7] = -1;
			}
		}
		return newPosY;
	}
	private int[] getposselectz(Object z, String operation){
		int[] newPosZ = {0,1,2,3,4,5,6,7};
		Object midz = findMid(topLeftFront.getZ(), bottomRightBack.getZ());
		if(operation.equals(">") || operation.equals(">=")) {
			if(comparing(z,midz) >= 0) {
				newPosZ[0] = -1;
				newPosZ[1] = -1;
				newPosZ[2] = -1;
				newPosZ[3] = -1;
			}
		}
		if(operation.equals("<") || operation.equals("<=")) {
			if(comparing(z,midz) <= 0) {
				newPosZ[4] = -1;
				newPosZ[5] = -1;
				newPosZ[6] = -1;
				newPosZ[7] = -1;
			}
		}
		return newPosZ;
	}

	private boolean checkchild(Object x, Object y, String operation) {
		boolean flag = false;

		if(operation.equals(">")) {
			return comparing(x,y) > 0;
		}
		if(operation.equals(">=")) {
			return comparing(x,y) >= 0;
		}
		if(operation.equals("<")) {
			return comparing(x,y) < 0;
		}
		if(operation.equals("<=")) {
			return comparing(x,y) <= 0;
		}
		if(operation.equals("=")) {
			return comparing(x,y) == 0;
		}
		if(operation.equals("!=")) {
			return comparing(x,y) != 0;
		}

		return flag;
	}

	public Vector<Object[]> selectequal(Object x, Object y, Object z ,String xop ,String yop, String zop) {
//		if (comparing(x, topLeftFront.getX()) > 0 || comparing(x, bottomRightBack.getX()) < 0
//				|| comparing(y, topLeftFront.getY()) > 0 || comparing(y, bottomRightBack.getY()) < 0
//				|| comparing(z, topLeftFront.getZ()) > 0 || comparing(z, bottomRightBack.getZ()) < 0) {
//			return null;
//			}
		Object midx = findMid(topLeftFront.getX(), bottomRightBack.getX());
		Object midy = findMid(topLeftFront.getY(), bottomRightBack.getY());
		Object midz = findMid(topLeftFront.getZ(), bottomRightBack.getZ());

		//		int [] oldPos = {0,1,2,3,4,5,6,7};
		int [] newPosX = {0,1,2,3,4,5,6,7};
		int [] newPosY = {0,1,2,3,4,5,6,7};
		int [] newPosZ = {0,1,2,3,4,5,6,7};

		if (xop.equals("=")) {
			if (comparing(x, midx ) < 0 ) {
				newPosX[1] = -1;
				newPosX[2] = -1;
				newPosX[5] = -1;
				newPosX[6] = -1;
			}
			else {
				newPosX[0] = -1;
				newPosX[3] = -1;
				newPosX[4] = -1;
				newPosX[7] = -1;
			}

		}
		if(yop.equals("=")) {
			if (comparing(y, midy ) < 0 ) {
				newPosY[2] = -1;
				newPosY[3] = -1;
				newPosY[6] = -1;
				newPosY[7] = -1;
			}
			else {
				newPosY[0] = -1;
				newPosY[1] = -1;
				newPosY[4] = -1;
				newPosY[5] = -1;
			}
		}
		if(zop.equals("=")) {
			if (comparing(z, midz ) < 0 ) {
				newPosZ[4] = -1;
				newPosZ[5] = -1;
				newPosZ[6] = -1;
				newPosZ[7] = -1;
			}
			else {
				newPosZ[0] = -1;
				newPosZ[1] = -1;
				newPosZ[2] = -1;
				newPosZ[3] = -1;
			}
		}
		Vector<Integer> newPos = new Vector<>();
		Vector<Object[]>result = new Vector<>();
		for(int i = 0 ; i < 8 ;  i++) {
			if(newPosX[i] != -1 && newPosY[i] != -1 && newPosZ[i] != -1) {
				newPos.add(i);
			}
		}

		//int pos = findPosition(x, y, z, midx, midy, midz);
		for (int i =0 ; i < newPos.size() ; i ++) {
			int pos = newPos.get(i);
			if (children[pos].point == null) {
				Vector<Object[]> object = children[pos].selectequal(x, y, z,xop,yop,zop);
				for (int j = 0 ; j< object.size();j++) {
					result.add(object.get(j));
				}
				continue ;
			}

			if (children[pos].point.isNullified()) {
				continue ;
			}
			for (int j =0 ; j < children[pos].refrences.size() ; j ++) {

				if (checkchild(children[pos].refrences.get(j)[0],x,xop) &&
						checkchild(children[pos].refrences.get(j)[1],y,yop) &&
						checkchild(children[pos].refrences.get(j)[2],z,zop)) {
					result.add(children[pos].refrences.get(j));
				}
			}
		}
		return result;
	}
}








