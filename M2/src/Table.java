import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import Exceptions.DBAppException;
import Exceptions.DuplicateException;
import Exceptions.NotFoundException;

public class Table implements java.io.Serializable {
	String name;
	Vector<Object[]> PagesID;//Add PK

	public Table(String name) {
		this.name = name;
		this.PagesID = new Vector<Object[]>();
	}

	public ArrayList<Object> insert(Hashtable<String,Object> tuple) throws IOException, DuplicateException {
		ArrayList<Object> result = new ArrayList<Object>(); 
		Object value = getPk(tuple);

		if(PagesID.size()==0 ){//If No pages were created
			Page page = new Page(name ,0);
			page.insertintopage(tuple);
			Object[] pageArray = {page.getID(), page.getMin() , page.getMax() , false };
			PagesID.add(pageArray);
			savepage(page.getID(), page);
			result.add(page.getID());
		}
		else if(PagesID.size() == 1 && ((boolean)PagesID.get(0)[3] == false || comparing(value,PagesID.get(0)[2])<0)) {//if first page is not full or value is less than max
			Page page = loadpage((int)PagesID.get(0)[0]);	
			Hashtable<String,Object> overflow =page.insertintopage(tuple);
			ArrayUpdateandEdit(PagesID.get(0), page);
			savepage(page.getID(), page);
			result.add(page.getID());
			if( overflow !=null ){
				if(page.getID()==(int)PagesID.get(PagesID.size()-1)[0]) {
					Page newpage = new Page(name , (int)PagesID.get(PagesID.size()-1)[0]+1);
					newpage.insertintopage(overflow);
					Object[] pageArray = {newpage.getID(), newpage.getMin() , newpage.getMax() , false };
					PagesID.add(pageArray);
					savepage(newpage.getID(), newpage);
					Object[] inres = {overflow ,newpage.getID()};
					result.add(inres);
				}
			}
		}

		else if(comparing(value,PagesID.get(PagesID.size()-1)[1])>0) {
			if((Boolean)PagesID.get(PagesID.size()-1)[3]&&comparing(value,PagesID.get(PagesID.size()-1)[2])>0){//If Bigger than last page max
				
				Page page = new Page(name ,(int)PagesID.get(PagesID.size()-1)[0]+1);
				page.insertintopage(tuple);
				Object[] pageArray = {page.getID(), page.getMin() , page.getMax() , false };
				PagesID.add(pageArray);
				savepage(page.getID(), page);
				result.add(page.getID());
			}
			else{//Within range of last page
				Page page = loadpage((int)PagesID.get(PagesID.size()-1)[0]);	
				Hashtable<String,Object> overflow =page.insertintopage(tuple);

				ArrayUpdateandEdit(PagesID.get(PagesID.size()-1), page);
				savepage(page.getID(), page);
				result.add(page.getID());
				if( overflow !=null ){// Create new page
					Page newpage = new Page(name , (int)PagesID.get(PagesID.size()-1)[0]+1);
					newpage.insertintopage(overflow);
					Object[] pageArray = {newpage.getID(), newpage.getMin() , newpage.getMax() , false };
					PagesID.add(pageArray);

					savepage(newpage.getID(), newpage);
					Object[] inres = {overflow ,newpage.getID()};
					result.add(inres);
				}
			}
		}
		else if (comparing(value, PagesID.get(0)[1] ) < 0 ) { //Value is less than min of first page
			Page page = loadpage((int)PagesID.get(0)[0]);	
			Hashtable<String,Object> overflow =page.insertintopage(tuple);
			ArrayUpdateandEdit(PagesID.get(0), page);
			savepage(page.getID(), page);
			result.add(page.getID());
			if( overflow !=null ){
				ArrayList<Object> temp = insert(overflow);
				Object[] o = {overflow , temp.get(0)};
				result.add(o);
				for (int i = 1; i < temp.size();i++) {
					result.add(temp.get(i));
				}
			}	
		}
		else{
			for(int i=0 ; i<PagesID.size()-1 ; i++){
				if(comparing(value , (PagesID.get(i))[1])>0 && comparing(value , (PagesID.get(i+1))[1])<0 ){//within range of page 
					if(comparing(value, PagesID.get(i)[2] )>0 && (Boolean)PagesID.get(i)[3]==true) { // bigger than max and page is.
						i++;
					}
					Page page = loadpage((int)PagesID.get(i)[0]);	
					Hashtable<String,Object> overflow =page.insertintopage(tuple);
					ArrayUpdateandEdit(PagesID.get(i), page);
					// System.out.println(PagesID.get(page.getID())[0] + " " + PagesID.get(page.getID())[1]  + " " + PagesID.get(page.getID())[2] + " " + PagesID.get(page.getID())[3]);
					savepage(page.getID(), page);
					result.add(page.getID());
					if( overflow !=null ){
						ArrayList<Object> temp = insert(overflow);
						Object[] o = {overflow , temp.get(0)};
						result.add(o);
						for (int j = 1; j < temp.size();j++) {
							result.add(temp.get(j));
						}
					}
					break ;
				} 
			}
		}
		return result;
	}

	public void ArrayUpdateandEdit(Object[] Array , Page page){ // Aghadnahom
		Array[1] = page.getMin();
		Array[2]=page.getMax();
		Array[3]=page.isFull();	
	} 




	public Object getPk(Hashtable<String,Object> tuple) throws IOException{

		BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
		String line = br.readLine();
		Object value = null;

		while (line != null) {
			String[] content = line.split(",");
			if(this.name.equals(content[0])) {
				if(content[3].equals("True") ) {
					value = tuple.get(content[1]);
				}
			}
			line = br.readLine();
		}
		br.close();

		return value;
	}


	private Double comparing(Object value , Object compared) {
		Double difference = 0.0;

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

	public int delete (Hashtable<String,Object> tuple) throws IOException, NotFoundException{
		int deleted = 0 ;

		if (tuple.size() == 0 ) { // if input is empty delete all pages
			for (int i = 0 ; i < PagesID.size() ; i ++) {
				File myObj = new File(name + PagesID.get(i)[0] + ".class");
				PagesID.remove(i);
				i--;
				myObj.delete();
			}
			return -1 ;
		}

		Object pk = null;
		BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
		String line = br.readLine();
		while (line != null) {
			String[] content = line.split(",");
			if(this.name.equals(content[0])) {

				if(content[3].equals("True")) {
					pk = content[1];
				}
			}
			line = br.readLine();
		}
		br.close();

		Enumeration<String> keys1 = tuple.keys();
		boolean flag = false;
		while(keys1.hasMoreElements()) {
			String key = keys1.nextElement();
			if(key.equals(pk)) {
				flag = true;
				break;
			}
		}

		if(flag) {
			Object value = getPk(tuple);
			for(int i=0 ; i<PagesID.size() ; i++){
				if(comparing(value , (PagesID.get(i))[1])>=0 && comparing(value , (PagesID.get(i))[2])<=0 ){
					Page page = loadpage((int)PagesID.get(i)[0]);	
					int oldsize = page.getPage().size() ;
					int size =page.deletefrompage(tuple);
					deleted += oldsize - size ;
					if(size==0){ 
						File myObj = new File(name + PagesID.get(i)[0] + ".class");
						PagesID.remove(i);
						myObj.delete();
					}
					else{
						ArrayUpdateandEdit(PagesID.get(i), page);
						savepage(page.getID(), page);
					}
					break;
				} 
			}
		}
		else {
			for(int i=0 ; i<PagesID.size() ; i++){
				Page page = loadpage((int)PagesID.get(i)[0]);
				int oldsize = page.getPage().size() ;
				int size =page.deletefrompage(tuple);
				deleted += oldsize - size ;
				if(size==0){
					File myObj = new File(name + PagesID.get(i)[0] + ".class");
					PagesID.remove(i);
					myObj.delete();
					i--;
				}
				else{
					ArrayUpdateandEdit(PagesID.get(i), page);
					savepage(page.getID(), page);
				}
			}
		}
		return deleted ;
	}

	public Object[] update(String strTableName, String strClusteringKeyValue, 
			Hashtable<String,Object> htblColNameValue ) throws DBAppException, IOException, ParseException {

		String type =null;
		Object value = strClusteringKeyValue;

		BufferedReader br = new BufferedReader(new FileReader("metadata.csv"));
		String line = br.readLine();
		while (line != null) {
			String[] content = line.split(",");
			if(this.name.equals(content[0])) {
				if(content[3].equals("True")) {
					type = content[2];
				}
			}
			line = br.readLine();
		}
		br.close();

		if(type.equals("java.lang.Integer")) {
			value = (java.lang.Integer)Integer.parseInt(strClusteringKeyValue);
		}

		if(type.equals("java.lang.Double")) {
			value =(java.lang.Double) Double.parseDouble(strClusteringKeyValue);
		}

		if(type.equals("java.util.Date")) {
			value =new Date(strClusteringKeyValue);
		}

		if(type.equals("java.lang.String")) {
			value = (java.lang.String) strClusteringKeyValue;
		}

		Object[] newRow = null;
		boolean flag = false; 
		for(int i=0 ; i<PagesID.size() ; i++){
			if(comparing(value , (PagesID.get(i))[1])>=0 && comparing(value , (PagesID.get(i))[2])<=0  ){
				flag = true;
				Page page = loadpage((int)PagesID.get(i)[0]);	
				newRow = page.updateTable( value, htblColNameValue);
				ArrayUpdateandEdit(PagesID.get(i), page);
				savepage(page.getID(), page);
				break ;
			}
		}
		if(!flag){
			throw new NotFoundException();
		}
		
		return newRow ; // { pk , id ,page.get(position) }
		
	}

	public void savepage(Integer key,Page page) throws IOException {
		try(FileOutputStream fout = new FileOutputStream( name + key + ".class");
				ObjectOutputStream oos = new ObjectOutputStream(fout);){
			oos.writeObject(page);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public Page loadpage(Integer key) {
		try(FileInputStream fiut = new FileInputStream( name + key + ".class");
				ObjectInputStream ois = new ObjectInputStream(fiut);){
			return (Page)ois.readObject();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public String getName() {
		return name;
	}



	public Vector<Hashtable<String, Object>> select(SQLTerm sqlTerm) {

		Vector<Hashtable<String, Object>> result = new Vector<Hashtable<String, Object>>() ;
		for(int i=0 ; i<PagesID.size() ; i++){
			Page page = loadpage((int)PagesID.get(i)[0]);	
			page.selectFromPage( sqlTerm , result);

		}


		return result;
	}

	public void select( SQLTerm sqlTerm1, String operator 
			,Vector<Hashtable<String, Object>> v) {



		for(int i=0 ; i<PagesID.size() ; i++){
			Page page = loadpage((int)PagesID.get(i)[0]);	
			page.selectFromPage( sqlTerm1 , operator , v );

		}



	}


	public void createIndex(Octree index  , String x , String y , String z) {

		for(int i=0 ; i<PagesID.size() ; i++){
			Page page = loadpage((int)PagesID.get(i)[0]);
			page.createIndex(index ,x ,y,z);

		}

	}


	public Hashtable<String , Object > getRow(Object[] refrence){ //find the row by octree

		Page page = loadpage(  (int)refrence[1]);
		
		return page.getRow( refrence[0]);
	}

	public Hashtable<String, Object> updateIndex(Object[] result, Hashtable<String, Object> htblColNameValue) {
		
		Page page = loadpage(  (int)result[1]);
		
		return page.updateIndex(result[0] ,   htblColNameValue);
		
		
	}

	public void deleteindex(Object[] refrence) {
		Page page = loadpage(  (int)refrence[1]);
		page.deleteindex(refrence[0]);
	}
	
	

}
