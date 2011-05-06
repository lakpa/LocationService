package uk.ac.tvu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import support.ByteBuffer;
import support.DataKeys;
import support.KNNMain;
import support.KNNModel;
import support.MIMETypeConstantsIF;
import support.BluetoothDeviceModel;

/**
 * Servlet implementation class LocationFinder
 */
public class LocationFinderServlet extends HttpServlet {
	private String category = "";
	public static String MAC_ADD_ROOM1 = "00:19:0E:08:08:B7";
	public static String MAC_ADD_ROOM2 = "00:19:0E:08:04:EA";
	public static String MAC_ADD_ROOM3 = "00:19:0E:08:06:F6";
	private String ss1, ss2, ss3;

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		ServletOutputStream sos = response.getOutputStream();
		response.setContentType(MIMETypeConstantsIF.PLAIN_TEXT_TYPE);
		sos.write("This is location finder service".getBytes());
		sos.flush();
		sos.close();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override 
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		ByteBuffer inputBB = new ByteBuffer(request.getInputStream());
		ByteBuffer outputBB = null;

		try {
			System.out.println("Extracting hash table from the request");
			ObjectInputStream ois = new ObjectInputStream(
					inputBB.getInputStream());
			@SuppressWarnings("unchecked")
			Hashtable<String, List<BluetoothDeviceModel>> input = (Hashtable<String, List<BluetoothDeviceModel>>) ois.readObject();
			
			
			if (input != null) {
				System.out.println("got the query instance vector " + input);
			}
			Object retVal = processInput(input);
			System.out.println("created response hashtable, sending it back to the client");
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(retVal);
			
			outputBB = new ByteBuffer(baos.toByteArray());
			System.out.println("sent response back to the client...");
			
		} catch (Exception e) {
			System.err.println(e);
		}

		ServletOutputStream sos = response.getOutputStream();

		  if (outputBB != null) {
		    response.setContentType("application/octet-stream");
		    response.setContentLength(outputBB.getSize());
		    System.out.println(outputBB.toByteArray().toString());
		    sos.write(outputBB.getBytes());
		  }
		  else {
		    response.setContentType("application/octet-stream");
		    response.setContentLength(inputBB.getSize());
		    sos.write(inputBB.getBytes());
		  }

		  sos.flush();
		  sos.close();
	}
	
	private String getCategory(Hashtable<String, List<BluetoothDeviceModel>> input) {
		KNNMain main = new KNNMain();
		if (main.connectToDB()) {
			System.out.println("Connected to db successfully");
		}
		
		List<BluetoothDeviceModel> modelList = input.get("deviceList");
		
		for(int i=0; i<modelList.size(); i++) {
			BluetoothDeviceModel model = modelList.get(i);
			if (model.getMacAddress().equals(MAC_ADD_ROOM1)) {
				ss1 = model.getSignalStrength();
			} else if (model.getMacAddress().equals(MAC_ADD_ROOM2)) {
				ss2 = model.getSignalStrength();
			}  else if (model.getMacAddress().equals(MAC_ADD_ROOM3)) {
				ss3 = model.getSignalStrength();
			} 
		}
		
		KNNModel km = new KNNModel();
		km.setRoom1(Integer.parseInt(ss1));
		km.setRoom2(Integer.parseInt(ss2));
		km.setRoom3(Integer.parseInt(ss3));
		category = main.classifiedQueryInstance(main.getTrainingKNNData(), km,
				2);
		return category;
	}

	private Hashtable<DataKeys, Serializable> processInput(
			Hashtable<String, List<BluetoothDeviceModel>> input) {
		Hashtable<DataKeys, Serializable> retval = new Hashtable<DataKeys, Serializable>();
		String category = getCategory(input);		
		retval.put(DataKeys.message, category);
		return retval;
	}	
}