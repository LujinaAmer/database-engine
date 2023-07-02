import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import Exceptions.DBAppException;
import Exceptions.DuplicateException;
import Exceptions.NotFoundException;
import Exceptions.OutOfBoundsException;


public class Page implements java.io.Serializable{
	private String tablename;
	private Vector<Hashtable<String,Object>> page;
	private String pk;
	private int capacity;
	private int id;

	public Page(String strTableName , int id) throws IOException {

		this.tablename = strTableName;

		BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
		String line = br.readLine();
		while (line != null) {
			String[] content = line.split(",");
			if(this.tablename.equals(content[0])) {

				if(content[3].equals("True")) {
					pk = content[1];
				}
			}
			line = br.readLine();
		}
		br.close();


		Properties prop = new Properties();
		String fileName = "src/resources/DBApp.config";
		try (FileInputStream fis = new FileInputStream(fileName)) {
			prop.load(fis);
		} catch (FileNotFoundException ex) {

		} catch (IOException ex) {

		}	

		capacity = Integer.parseInt(prop.getProperty("MaximumRowsCountinTablePage"));

		page = new Vector<Hashtable<String,Object>>(capacity);

		this.id= id ;

	}

	public String getTablename() {

		return tablename;
	}


	/**
	 * @return the page
	 */
	public Vector<Hashtable<String, Object>> getPage() {
		return page;
	}

	public Hashtable<String,Object> insertintopage(Hashtable<String,Object> tuple) 
			throws IOException , DuplicateException {

		Hashtable<String,Object> lasttuple = null;
		if(page.isEmpty()) {
			page.insertElementAt(tuple, 0);
			return lasttuple;

		}

		Object value = tuple.get(pk);


		int l = 0;
		int r = page.size()-1;
		int position =  l+(r-l)/2;
		while(r>=l) {
			if(comparing(value, page.get(position).get(pk))==0){
				throw new DuplicateException("Duplicate Detected") ;
			}

			if(position==0 && (comparing(value,page.get(position).get(pk)) < 0)){
				break;
			}
			if((comparing(value,page.get(position).get(pk)) > 0) && position == page.size()-1 && position <capacity )  {
				position ++;
				break;	
			}
			if(position>0 && comparing(value,page.get(position).get(pk)) < 0 && comparing(value,page.get(position-1).get(pk)) > 0) {
				break;
			}
			if(position < page.size()-1 && comparing(value,page.get(position).get(pk)) > 0 && comparing(value,page.get(position+1).get(pk)) < 0) {
				position ++;
				break;
			}
			if(comparing(value,page.get(position).get(pk)) < 0) {
				r=position-1;
				position = l+(r-l)/2;
			}
			if(comparing(value,page.get(position).get(pk)) > 0) {
				l=position+1;
				position = l+(r-l)/2;
			}

		}

		if(page.size() == capacity) { //to be discussed later and continued

			lasttuple = page.get(page.size()-1);
			page.remove(page.size()-1);
		}
		page.insertElementAt(tuple, position);

		return lasttuple;

	}


	private Double comparing(Object value , Object compared) {
		Double difference = 0.0;
		if(value.toString().equals("Null") || compared.toString().equals("Null")) {
			return null;
		}
		if(value.getClass().getName().equals("java.lang.Integer")) {
			difference += (java.lang.Integer)value - (java.lang.Integer)compared;
			return difference;
		}

		if(value.getClass().getName().equals("java.lang.Double")) {
			difference = (java.lang.Double)value - (java.lang.Double)compared;
			return difference;
		}

		if(value.getClass().getName().equals("java.util.Date")) {
			difference += ((java.util.Date)value).compareTo((java.util.Date)compared);
			return difference;
		}

		if(value.getClass().getName().equals("java.lang.String")) {
			difference += ((java.lang.String)value).compareTo((java.lang.String)compared);
			return difference;
		}
		return difference;
	}

	private int binarysearch(Object value , int l , int r ) {

		if (r>=l) {

			int mid = l + (r - l) / 2;

			if (comparing(page.get(mid).get(pk) , value)==0)
				return mid;

			if (comparing(page.get(mid).get(pk) , value)>0)
				return binarysearch(value, l, mid - 1);

			return binarysearch(value, mid + 1, r);
		}

		return -1;

	}

	public int deletefrompage(Hashtable<String,Object> tuple) throws NotFoundException{
		boolean PKflag = false;
		Enumeration<String> keys1 = tuple.keys();
		while(keys1.hasMoreElements()) {
			String key = keys1.nextElement();
			if(key.equals(pk)) {
				PKflag = true;
				break;
			}
		}

		if(PKflag) {
			int index = binarysearch(tuple.get(pk), 0, page.size()-1);
			if(index == -1) {
				return page.size();
			}
			else {
				boolean flag = true;
				Enumeration<String> keys = tuple.keys();
				while (keys.hasMoreElements() && flag == true) {
					String key = keys.nextElement();
					flag = false;
					if( comparing(tuple.get(key),page.get(index).get(key)) != null && comparing(tuple.get(key),page.get(index).get(key)) == 0.0) {
						flag = true;
					}
				}
				if(flag) {
					page.remove(index);
				}
			}
		}
		else {
			for(int i = 0 ; i<page.size();i++) {
				boolean flag = true;
				Enumeration<String> keys = tuple.keys();
				while (keys.hasMoreElements() && flag == true) {
					String key = keys.nextElement();
					flag = false;
					if( comparing(tuple.get(key),page.get(i).get(key)) != null && comparing(tuple.get(key),page.get(i).get(key)) == 0.0 ) {
						flag = true;
					}
				}
				if(flag) {
					page.remove(i);
					i--;
				}
			}
		}	

		return page.size();

	}

	public Object[] updateTable(Object PkValue, 
			Hashtable<String,Object> htblColNameValue ) throws DBAppException, IOException, ParseException {

		int position = binarysearch(PkValue, 0, page.size()-1);

		if(position<0){
			throw new NotFoundException("Value Not Found");
		}
		else{
			Enumeration<String> keys = htblColNameValue.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				page.get(position).put(key,htblColNameValue.get(key) );
				//System.out.println(page.get(position).get("gpa"));
			}
		}
		Object[] result = {pk , id ,page.get(position) };
		return result;
	}

	public int getID() {
		return id;
	}

	public Object getMin(){
		return page.get(0).get(pk);
	}

	public Object getMax(){
		return page.get(page.size()-1).get(pk);
	}

	public boolean isFull(){
		return page.size()==capacity;
	}

	public void selectFromPage(SQLTerm sqlTerm, Vector<Hashtable<String, Object>> result) {

		for(int i = 0 ; i < page.size() ; i++) {
			if (sqlTerm._strOperator.equals(">")) {
				if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) > 0) {
					result.add(page.get(i));
				}
			}
			if (sqlTerm._strOperator.equals(">=")) {
				if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) >= 0) {
					result.add(page.get(i));
				}
			}
			if (sqlTerm._strOperator.equals("<")) {
				if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) < 0) {
					result.add(page.get(i));
				}
			}
			if (sqlTerm._strOperator.equals("<=")) {
				if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) <= 0) {
					result.add(page.get(i));
				}
			}
			if (sqlTerm._strOperator.equals("!=")) {
				if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) != 0) {
					result.add(page.get(i));
				}
			}
			if (sqlTerm._strOperator.equals("=")) {
				if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) == 0) {
					result.add(page.get(i));
				}
			}

		}//for end

	}

	public void selectFromPage(SQLTerm sqlTerm, String operator,
			Vector<Hashtable<String, Object>> result) { 

		// in case of "and", we will loop over the vector ,adding what satisfies the SQL term in temp
		if (operator.equals("AND")) { 
			Vector<Hashtable<String, Object>> temp = new Vector<Hashtable<String, Object>>() ;

			for(int i =0 ; i < result.size() ; i++) {
				
				if (sqlTerm._strOperator.equals(">")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) > 0) {
						temp.add(result.get(i));
					}
				}
				if (sqlTerm._strOperator.equals(">=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) >= 0) {
						temp.add(result.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("<")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) < 0) {
						temp.add(result.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("<=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) <= 0) {
						temp.add(result.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("!=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) != 0) {
						temp.add(result.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) == 0) {
						temp.add(result.get(i));
					}
				}
			}
			result = temp ;

		}
		else if (operator.equals("OR")) {
			// in case of "or", we will loop over the page ,adding what satisfies the SQL term to result
			// only if it result !contains it
			for(int i = 0 ; i < page.size() ; i++) {
				
				if (sqlTerm._strOperator.equals(">")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) > 0
							&& !result.contains(page.get(i)) ) {
						result.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals(">=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) >= 0
							&& !result.contains(page.get(i)) ) {
						result.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("<")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) < 0
							&& !result.contains(page.get(i)) ) {
						result.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("<=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) <= 0
							&& !result.contains(page.get(i)) ) {
						result.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("!=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) != 0
							&& !result.contains(page.get(i)) ) {
						result.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) == 0
							&& !result.contains(page.get(i)) ) {
						result.add(page.get(i));
					}
				}

			}//for end


		}
		else if (operator.equals("XOR")) {

			// in case of "XOR", we will loop over the page ,adding what satisfies the SQL term to temp
			// then we merge temp and result , considering the duplicates
			
			Vector<Hashtable<String, Object>> temp = new Vector<Hashtable<String, Object>>() ;
			for(int i = 0 ; i < page.size() ; i++) {
				if (sqlTerm._strOperator.equals(">")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) > 0  ) {
						temp.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals(">=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) >= 0 ) {
						temp.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("<")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) < 0 ) {
						temp.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("<=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) <= 0 ) {
						temp.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("!=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) != 0 ) {
						temp.add(page.get(i));
					}
				}
				if (sqlTerm._strOperator.equals("=")) {
					if (comparing(page.get(i).get(sqlTerm._strColumnName) , sqlTerm._objValue) == 0 ) {
						temp.add(page.get(i));
					}
				}

			}//for end

			for(int i =0 ; i < temp.size() ; i++) {
				if(result.contains(temp.get(i))) { // 1 xor 1 = 0   ( 0 xor  0 =0 so we don't care for that case)
					result.remove(temp.get(i));
				}
				else { // 1 xor 0 = 1    or   0 xor 1 = 1
					result.add(temp.get(i));
				}
				
			}
		}//end of XOR case


	}// end of selectFromPage()




	public void createIndex(Octree index  , String x , String y , String z) {
		
		for (Hashtable<String, Object> row : page) { // insert table to Octree
			Vector<Object[]> v = new Vector<>();
			Object[] obj = { row.get(pk) , id};
			v.add(obj);
			Object[] record = {row.get(x), row.get(y)  , row.get(z) ,v };
			try {
				index.insert( row.get(x), row.get(y)  , row.get(z) , record);
			} catch (OutOfBoundsException e) {
				
			}
			
		}
		
	}


	public Hashtable<String , Object > getRow(Object Octpk){
		
		int index = binarysearch( Octpk , 0, page.size()-1);
		return page.get(index) ;
		
	}

	public Hashtable<String, Object> updateIndex(Object pkVal, Hashtable<String, Object> htblColNameValue) {
		// TODO Auto-generated method stub
		int index = binarysearch( pkVal , 0, page.size()-1);
		
		
		Enumeration<String> keys = htblColNameValue.keys();
		while (keys.hasMoreElements() ) {
			String key = keys.nextElement();
			page.get(index).put( key , htblColNameValue.get(key) );
		}
		
		return page.get(index);
		
		
	}

	public void deleteindex (Object Octpk) {
		int index = binarysearch( Octpk , 0, page.size()-1);
		page.remove(index);
	}

}