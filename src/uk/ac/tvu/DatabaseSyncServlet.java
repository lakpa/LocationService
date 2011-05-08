package uk.ac.tvu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import support.ByteBuffer;
import support.KNNMain;
import support.KNNModel;
import support.DataKeys;
import support.MIMETypeConstantsIF;

/**
 * Servlet implementation class DatabaseSyncServlet.
 *
 * @author lakpa
 * @author Nazmul Idris
 */
public class DatabaseSyncServlet extends HttpServlet {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The main. */
	private KNNMain main = null;

	/**
	 * Instantiates a new database sync servlet.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public DatabaseSyncServlet() {
		super();

	}

	/**
	 * Do get.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException the servlet exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		ServletOutputStream sos = response.getOutputStream();
		response.setContentType(MIMETypeConstantsIF.PLAIN_TEXT_TYPE);
		sos.write("This is database sync service".getBytes());
		sos.flush();
		sos.close();
	}

	/**
	 * Do post.
	 *
	 * @param request the request
	 * @param response the response
	 * @throws ServletException the servlet exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		ByteBuffer inputBB = new ByteBuffer(request.getInputStream());
		ByteBuffer outputBB = null;
		
		main = new KNNMain();
		// connect database
		main.connectToDB();
		
		try {
			System.out.println("Extracting hash table from the request");
			ObjectInputStream ois = new ObjectInputStream(
					inputBB.getInputStream());
			@SuppressWarnings("unchecked")
			Hashtable<String, String> input = (Hashtable<String, String>) ois
					.readObject();

			List<KNNModel> listModel = main.getTrainingKNNData();
			System.out.println("got the query instance vector " + input);

			Object retVal = process(input, listModel);

			System.out
					.println("created response hashtable, sending it back to the client");

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
		} else {
			response.setContentType("application/octet-stream");
			response.setContentLength(inputBB.getSize());
			sos.write(inputBB.getBytes());
		}

		sos.flush();
		sos.close();
	}

	/**
	 * Process.
	 *
	 * @param dateVal the date val
	 * @param listModel the list model
	 * @return the hashtable
	 */
	private Hashtable<DataKeys, Serializable> process(
			Hashtable<String, String> dateVal,
			List<KNNModel> listModel) {
		Hashtable<DataKeys, Serializable> val = new Hashtable<DataKeys, Serializable>();
		KNNModel kModel = getNewData(dateVal, listModel);
		val.put(DataKeys.dataList, kModel);
		kModel = null;
		return val;
	}

	/**
	 * Gets the new data.
	 *
	 * @param input the input
	 * @param dbList the db list
	 * @return the new data
	 */
	private KNNModel getNewData(Hashtable<String, String> input,
			List<KNNModel> dbList) {
		KNNModel km = new KNNModel();
		List<KNNModel> returnList = new ArrayList<KNNModel>();
		String date = input.get("latestUpdatedDate");
		int date1 = Integer.parseInt(date);
		List<KNNModel> dbModelList = dbList;
		for (int j = 0; j < dbModelList.size(); j++) {
			KNNModel dbModel = dbModelList.get(j);
			if (date1 != 0) {
				if (dbModel.getDate() > date1) {
					returnList.add(dbModel);
				} 
			} else {
				returnList.add(dbModel);
			}
		}
		
		km.setKnnList(returnList);
		return km;
	}
}
