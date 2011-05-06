package support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author lakpa dindu
 * 
 */

public class KNNMain {

	private Connection connection = null;
	private List<KNNModel> knnList = null;
	private List<KNNModel> knnList1 = null;
	private List<Integer> assignedRank = null;
	private KNNModel knnModel = null;
	private String highestClassification = "";

	public List<KNNModel> getKnnList() {
		return knnList;
	}

	public void setKnnList(List<KNNModel> knnList) {
		this.knnList = knnList;
	}

	public KNNMain() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		knnList = new ArrayList<KNNModel>();
		assignedRank = new ArrayList<Integer>();
	}

	private void displaySQLErrors(SQLException e) {
		System.out.println("SQLException: " + e.getMessage());
		System.out.println("SQLState: " + e.getSQLState());
		System.out.println("VendorError: " + e.getErrorCode());
	}

	private Properties getConnectionProperties() {
		Properties prop = new Properties();
		prop.setProperty("user", "root");
		prop.setProperty("password", "password");
		return prop;
	}

	public boolean connectToDB() {
		try {
			connection = DriverManager.getConnection(
					"jdbc:mysql://localhost/Knn", getConnectionProperties());
		} catch (SQLException e) {
			displaySQLErrors(e);
		}
		return true;
	}

	public List<KNNModel> getTrainingKNNData() {
		knnList1 = new ArrayList<KNNModel>();
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * from ss_table");
			int i = 0;
			while (rs.next()) {
				knnModel = new KNNModel();
				knnModel.setRoom1(rs.getInt(1));
				knnModel.setRoom2(rs.getInt(2));
				knnModel.setRoom3(rs.getInt(3));
				knnModel.setClassification(rs.getString(4));
				knnModel.setDate(rs.getInt(5));
				knnList1.add(i, knnModel);
				i++;
			}
			rs.close();
			stmt.close();
			// connection.close();
		} catch (SQLException e) {
			displaySQLErrors(e);
		}
		return knnList1;
	}

	public String classifiedQueryInstance(List<KNNModel> knnModelList,
			KNNModel queryInstance, int k) {

		String category = "";
		List<KNNModel> knnList1 = setSquareDistance(knnModelList, queryInstance);
		knnList = setRank(knnList1);
		knnList = checkIncludedInNeighbourList(knnList, k);
		// printList(knnList1);
		int nearTC354 = 0, inTC357 = 0, inTC364 = 0, nearTC360 = 0, nearTC357 = 0;
		for (int i = 0; i < knnList.size(); i++) {
			if (knnList.get(i).getRank() <= k) {
				if (knnList.get(i).getRank() == 1)
					highestClassification = knnList.get(i).getClassification();
				
				if (knnList.get(i).getClassification().equals("In TC364")) {
					inTC364++;
				} else if (knnList.get(i).getClassification()
						.equals("In TC357")) {
					inTC357++;
				} else if (knnList.get(i).getClassification()
						.equals("Near TC360")) {
					nearTC360++;
				} else if (knnList.get(i).getClassification()
						.equals("Near TC357")) {
					nearTC357++;
				} else if (knnList.get(i).getClassification()
						.equals("Near TC354")) {
					nearTC354++;
				}
			}
		}

		System.out.println("After--");
		printList(knnList, k);
		int a[] = { nearTC354, inTC357, inTC364, nearTC360, nearTC357 };
		int index = findGreatest(a);
		if (index != -1) {
			switch (index) {
			case 0:
				category = "Near TC354";
				break;
			case 1:
				category = "In TC357";
				break;
			case 2:
				category = "In TC364";
				break;
			case 3:
				category = "Near TC360";
				break;
			case 4:
				category = "Near TC357";
				break;
			}
		} else {
			if (!highestClassification.equals("")) {
				return highestClassification;
			}
		}
		
		return category;
	}

	private int findGreatest(int... val) {
		int largest = val[0];
		boolean isLargest = false;
		int index = 0;
		for (int i = 0; i < val.length; i++) {
			if (i != 0) {
				if (val[i] > largest) {
					if (val[i] > 1) {	
						largest = val[i];
						index = i;
						isLargest = true;
					}
				}
			}
		}
		if (isLargest)
			return index;
		else 
			return -1;
	}

	private void printList(List<KNNModel> knnList, int k) {
		for (int i = 0; i < knnList.size(); i++) {
			KNNModel model = knnList.get(i);
			if (model.getRank() <= k) {
				System.out.println(model.getRoom1() + " " + model.getRoom2()
						+ " " + model.getRoom3() + " "
						+ model.getClassification() + " "
						+ model.getDistanceToQueryInstance() + " "
						+ model.getRank() + " " + model.isInNeighbourList());
			}
		}
	}

	private List<KNNModel> checkIncludedInNeighbourList(List<KNNModel> km, int k) {
		for (int i = 0; i < km.size(); i++) {
			if (km.get(i).getRank() <= k) {
				km.get(i).setInNeighbourList(true);
			}
		}
		return km;
	}

	private List<KNNModel> setSquareDistance(List<KNNModel> knnModelList,
			KNNModel queryInstance) {
		for (int i = 0; i < knnModelList.size(); i++) {
			int squareDistance = squareDistance(knnModelList.get(i),
					queryInstance);
			// set square distance to query instance
			knnModelList.get(i).setDistanceToQueryInstance(squareDistance);
		}
		return knnModelList;
	}

	private List<KNNModel> setRank(List<KNNModel> listModel) {
		int[] intArray = sortedArray(listModel);
		for (int i = 0; i < listModel.size(); i++) {
			for (int j = intArray.length - 1; j >= 0; j--)
				if (listModel.get(i).getDistanceToQueryInstance() == intArray[j]) {
					int rank = j + 1;
					if (!isDuplicate(rank)) {
						listModel.get(i).setRank(rank);
						assignedRank.add(rank);
					} else {
						rank += 1;
						listModel.get(i).setRank(rank);
					}
				}
		}
		return listModel;
	}

	private boolean isDuplicate(int rank) {
		for (int i = 0; i < assignedRank.size(); i++) {
			if (assignedRank.get(i).intValue() == rank)
				return true;
		}
		return false;
	}

	private int[] sortedArray(List<KNNModel> list) {
		int[] a = new int[list.size()];
		for (int i = 0; i < list.size(); i++)
			a[i] = list.get(i).getDistanceToQueryInstance();
		Arrays.sort(a);
		return a;
	}

	// return square distance between query string and training data
	private int squareDistance(KNNModel training, KNNModel queryInstance) {
		int room1, room2, room3;
		room1 = (int) Math.pow(
				(training.getRoom1() - queryInstance.getRoom1()), 2);
		room2 = (int) Math.pow(
				(training.getRoom2() - queryInstance.getRoom2()), 2);
		room3 = (int) Math.pow(
				(training.getRoom3() - queryInstance.getRoom3()), 2);
		return room1 + room2 + room3;
	}
}
