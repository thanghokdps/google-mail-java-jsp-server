package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.google.gson.*;
import DB.ConnectDB;
import Model.User;
import Model.Message;
//import Model.Attachment;

public class Server {
	public static void main(String[] args) throws Exception {
		new Server();
	}

	Vector<Menber> listUser = new Vector<>();

	public Server() {
		try {
			@SuppressWarnings("resource")
			ServerSocket server = new ServerSocket(9696);
			System.out.println("Server is open");
			while (true) {
				Socket socket = server.accept();
				Menber thread = new Menber(socket, this);
				listUser.add(thread);
				thread.start();
			}
		} catch (Exception e) {
			System.out.println("Error Server");
		}
	}
}

class Menber extends Thread {
	Socket socket;
	Server server;
	private InputStream is;
	private InputStreamReader isr;
	private BufferedReader br;
	private PrintWriter pw = null;
	Gson gson = new Gson();
	public Menber(Socket socket, Server server) {
		this.socket = socket;
		this.server = server;
		try {
			is = socket.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			if (pw == null) {
				pw = new PrintWriter(socket.getOutputStream());
			}
		} catch (Exception e) {
			System.out.println("Error User Thread");
		}
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {
			while (true) {
				String line = br.readLine();
				System.out.println(line + "");
				HashMap<String, String> request = new HashMap<>();
				request = gson.fromJson(line, request.getClass());
				HashMap<String, String> response = new HashMap<>();
				switch (request.get("command")) {
				case "authenticate":
					String username = request.get("username");
					String password = request.get("password");
					System.out.println("Login " + username + " " + password);
					User user = login(username, password);
					if (user == null) {
						response.put("status", "fail");
					} else {
						response.put("status", "success");
						response.put("id", Integer.toString(user.getid()));
						response.put("username", username);
						response.put("password", password);
						response.put("email",user.getemail());
					}
					break;
				case "register":
					String name = request.get("username");
					String pass = request.get("password");
					String email = request.get("email");
					User user1 = new User();
					user1.setusername(name);
					user1.setpassword(pass);
					user1.setemail(email);
					System.out.println("Register " + user1.getusername() + user1.getpassword() + user1.getemail());
					if (register(user1) == false) {
						response.put("status", "fail");
					} else {
						response.put("status", "success");
						response.put("username", name);
						response.put("password", pass);
					}
					break;
				case "insert_mess":
					int id_sender = Integer.parseInt(request.get("id_sender"));
					String namereceiver =(String)request.get("receiver");
					int id_receiver = getIDbyUsername(namereceiver);
					String title = request.get("title");
					String content = request.get("content");
					String create_at = request.get("create_at");
					Message message = new Message();
					message.setid_sender(id_sender);
					message.setid_receiver(id_receiver);
					message.settitle(title);
					message.setcontent(content);
					message.setcreate_at(create_at);
					System.out.println("Add " + message.getid_sender() + message.getid_receiver() + message.gettitle()
							+ message.getcontent() + message.getcreate_at());
					if (addMess(message) == false) {
						response.put("status", "fail");
					} else {
						response.put("status", "success");
						response.put("id_sender", String.valueOf(id_sender));
						response.put("id_receiver", String.valueOf(id_receiver));
						response.put("title", title);
						response.put("content", content);
						response.put("create_at", String.valueOf(create_at));
					}
					break;
				case "show_listMess":
					int id = Integer.parseInt(request.get("id"));
					ArrayList<Message> listmess = getAllMess(id);
					String list = gson.toJson(listmess);
					response.put("status", "success");
					response.put("id", String.valueOf(id));
					response.put("show_listmess", list);
					break;
				case "show_Mess":
					int mess_id = Integer.parseInt(request.get("id"));
					Message mess = getMess(mess_id);
					String mesString = gson.toJson(mess);
					response.put("status", "success");
					response.put("mess_id", String.valueOf(mess_id));
					response.put("show_Mess", mesString);
					break;
				case "delete_Mess":
					int noteId = Integer.parseInt(request.get("id"));
					if (deleteMess(noteId) > 0) {
						response.put("status", "success");
					} else {
						response.put("status", "fail");
					}
					break;
				case "forget_Password":
					String email1 = request.get("email");
					String pass1 = request.get("password");
					User user11 = UpdatePassword(email1, pass1);
					if (user11 == null) {
						response.put("status", "fail");
					} else {
						response.put("status", "success");
					}
					break;
				}
				String responseLine = gson.toJson(response);
				responseLine = responseLine + "\n";
				System.out.println(responseLine);
				pw.write(responseLine);
				pw.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
	}

	public User login(String username, String password) {
		Connection connect = ConnectDB.getConnection();
		String sql = "select * from user where username='" + username + "' and password='" + password + "'";
		PreparedStatement ps;
		try {
			ps = (PreparedStatement) connect.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				User user = new User();
				user.setid(rs.getInt("id"));
				user.setemail(rs.getString("email"));
				user.setusername(rs.getString("username"));
				user.setpassword(rs.getString("password"));
				connect.close();
				return user;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean register(User user) {
		Connection connect = ConnectDB.getConnection();
		String sql = "insert into user(username,password,email) values(?,?,?)";
		try {
			PreparedStatement ps = connect.prepareCall(sql);
			ps.setString(1, user.getusername());
			ps.setString(2, user.getpassword());
			ps.setString(3, user.getemail());
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean addMess(Message message) {
		Connection connect = ConnectDB.getConnection();
		String sql = "insert into message (id_sender, id_receiver, title, content, create_at) values(?,?,?,?,?)";
		try {
			PreparedStatement ps = connect.prepareCall(sql);
			ps.setInt(1, message.getid_sender());
			ps.setInt(2, message.getid_receiver());
			ps.setString(3, message.gettitle());
			ps.setString(4, message.getcontent());
			ps.setString(5, message.getcreate_at());
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public ArrayList<Message> getAllMess(int id) {
		ArrayList<Message> listmess = new ArrayList<>();
		Connection connect = ConnectDB.getConnection();
		String sql = "select * from message where id_receiver = ? order by id desc";
		try {
			PreparedStatement pst = connect.prepareStatement(sql);
			pst.setInt(1, id);
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				Message mess = new Message(rs.getInt("id"), rs.getInt("id_sender"), rs.getInt("id_receiver"),
						rs.getString("title"), rs.getString("content"), rs.getString("create_at"));
				listmess.add(mess);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return listmess;
	}

	public Message getMess(int id) {
		Message mess = new Message();
		Connection connect = ConnectDB.getConnection();
		String sql = "SELECT * FROM message WHERE id = ?";
		try {
			PreparedStatement pst = connect.prepareStatement(sql);
			pst.setInt(1, id);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				mess = new Message(rs.getInt("id"), rs.getInt("id_sender"), rs.getInt("id_receiver"),
						rs.getString("title"), rs.getString("content"), rs.getString("create_at"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return mess;
	}

	public int deleteMess(int id) {
		int result = 0;
		Connection connect = ConnectDB.getConnection();
		String sql = "DELETE from message where id=?";
		try {
			PreparedStatement pst = connect.prepareStatement(sql);
			pst.setInt(1, id);
			result = pst.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public String getUsernamebyID(int id) throws SQLException {
		ResultSet result = null;
		Connection connect = ConnectDB.getConnection();
		String sql = "select username from user where id=?";
		try {
			PreparedStatement pst = connect.prepareStatement(sql);
			pst.setInt(1, id);
			result = pst.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		result.next();
		return result.getString("username");
	}
	
	public int getIDbyUsername(String username) throws SQLException{
		ResultSet result = null;
		Connection connect = ConnectDB.getConnection();
		String sql = "select id from user where username=?";
		try {
			PreparedStatement pst = connect.prepareStatement(sql);
			pst.setString(1, username);
			result = pst.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		result.next();
		return result.getInt("id");
	}
	
	public User getUserbyID(int id) {
		Connection connect = ConnectDB.getConnection();
		String sql = "select * from user where id=" + id;
		PreparedStatement ps;
		try {
			ps = (PreparedStatement) connect.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				User user = new User();
				user.setid(rs.getInt("id"));
				user.setemail(rs.getString("email"));
				user.setusername(rs.getString("username"));
				user.setpassword(rs.getString("password"));
				connect.close();
				return user;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public User UpdatePassword(String email, String Pass) {
		Connection connect = ConnectDB.getConnection();
		String sql1 = "select * from user where email=?";
		String sql2 = "update user set password = ? where email = ?";
		try {
			PreparedStatement pst = connect.prepareStatement(sql2);
			pst.setString(1, Pass);
			pst.setString(2, email);
			System.out.println(pst.toString());
			if (pst.executeUpdate()==1) {
				PreparedStatement pst1 = connect.prepareStatement(sql1);
				pst1.setString(1, email);
				ResultSet rs = pst1.executeQuery();
				if (rs.next()) {
					User user = new User();
					user.setid(rs.getInt("id"));
					user.setemail(rs.getString("email"));
					user.setusername(rs.getString("username"));
					user.setpassword(rs.getString("password"));
					connect.close();
					return user;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
}