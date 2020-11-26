//JavaObjServer.java ObjectStream 기반 채팅 Server

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;

public class ServerMain extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	JTextArea textArea;
	private JTextField txtPortNumber;

	private ServerSocket socket; // 서버소켓
	private Socket client_socket; // accept() 에서 생성된 client 소켓
	private Vector UserVec = new Vector(); // 연결된 사용자를 저장할 벡터
	private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의
	private int[] RoomCnt = new int[5]; // 0은 방X 1부터 1번방 4번까지 4번방
	private String[] RoomStatus = { "대기중", "대기중", "대기중", "대기중" };

	// 인게임 데이터
	private int[][] money = new int[4][4];
	private int[][] land = new int[4][22]; // 빈땅 = 0, 앞자리는 유저1일 경우 1, 뒷자리는 건물 수로 이루어진 수, 11 = 유저1의 건물1개, 22 = 유저2의 건물 2개
	private String[][] RoomUsers = new String[4][4];

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerMain frame = new ServerMain();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ServerMain() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 338, 440);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 10, 300, 298);
		contentPane.add(scrollPane);

		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);

		JLabel lblNewLabel = new JLabel("Port Number");
		lblNewLabel.setBounds(13, 318, 87, 26);
		contentPane.add(lblNewLabel);

		txtPortNumber = new JTextField();
		txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
		txtPortNumber.setText("30000");
		txtPortNumber.setBounds(112, 318, 199, 26);
		contentPane.add(txtPortNumber);
		txtPortNumber.setColumns(10);

		JButton btnServerStart = new JButton("Server Start");
		btnServerStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
				} catch (NumberFormatException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				AppendText("Chat Server Running..");
				btnServerStart.setText("Chat Server Running..");
				btnServerStart.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
				txtPortNumber.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다
				AcceptServer accept_server = new AcceptServer();
				accept_server.start();
			}
		});
		btnServerStart.setBounds(12, 356, 300, 35);
		contentPane.add(btnServerStart);
	}

	// 새로운 참가자 accept() 하고 user thread를 새로 생성한다.
	class AcceptServer extends Thread {
		@SuppressWarnings("unchecked")
		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					AppendText("Waiting new clients ...");
					client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
					AppendText("새로운 참가자 from " + client_socket);
					// User 당 하나씩 Thread 생성
					UserService new_user = new UserService(client_socket);
					UserVec.add(new_user); // 새로운 참가자 배열에 추가
					new_user.start(); // 만든 객체의 스레드 실행
					AppendText("현재 참가자 수 " + UserVec.size());
				} catch (IOException e) {
					AppendText("accept() error");
					// System.exit(0);
				}
			}
		}
	}

	public void AppendText(String str) {
		// textArea.append("사용자로부터 들어온 메세지 : " + str+"\n");
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	public void AppendObject(ChatMsg msg) {
		// textArea.append("사용자로부터 들어온 object : " + str+"\n");
		textArea.append("code = " + msg.code + "\n");
		textArea.append("id = " + msg.UserName + "\n");
		textArea.append("data = " + msg.data + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	// User 당 생성되는 Thread
	// Read One 에서 대기 -> Write All
	class UserService extends Thread {
		private InputStream is;
		private OutputStream os;
		private DataInputStream dis;
		private DataOutputStream dos;

		private ObjectInputStream ois;
		private ObjectOutputStream oos;

		private Socket client_socket;
		private Vector user_vc;
		public String UserName = "";
		public String UserStatus;
		public int myRoomNumber;

		public UserService(Socket client_socket) {
			// TODO Auto-generated constructor stub
			// 매개변수로 넘어온 자료 저장
			this.client_socket = client_socket;
			this.user_vc = UserVec;
			try {
				oos = new ObjectOutputStream(client_socket.getOutputStream());
				oos.flush();
				ois = new ObjectInputStream(client_socket.getInputStream());
			} catch (Exception e) {
				AppendText("userService error");
			}
		}

		public void Login() {
			AppendText("새로운 참가자 " + UserName + " 입장.");
			WriteOne("Welcome to Java chat server\n");
			WriteOne(UserName + "님 환영합니다.\n"); // 연결된 사용자에게 정상접속을 알림
			String msg = "[" + UserName + "]님이 입장 하였습니다.\n";
			WriteOthers(msg); // 아직 user_vc에 새로 입장한 user는 포함되지 않았다.

			for (int i = 1; i <= 2; i++) {
				ChatMsg obcm = new ChatMsg(UserName, "602", i + " " + RoomCnt[i]);
				WriteAllObject(obcm);
			}
		}

		public void Logout() {
			String msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
			UserVec.removeElement(this); // Logout한 현재 객체를 벡터에서 지운다
			WriteAll(msg); // 나를 제외한 다른 User들에게 전송
			this.client_socket = null;
			AppendText("사용자 " + "[" + UserName + "] 퇴장. 현재 참가자 수 " + UserVec.size());
		}

		// 게임방 입장 600(게임방 데이터 전송)
		public void RoomLogin(int RoomNumber) {
			ChatMsg obcm;
			if (RoomStatus[RoomNumber].equals("게임중")) {
				AppendText(UserName + " : " + RoomNumber + "번방 입장 실패(게임중).");
				obcm = new ChatMsg("SERVER", "444", "게임이 이미 진행 중입니다.");
			} else if (RoomCnt[RoomNumber] == 4) {
				AppendText(UserName + " : " + RoomNumber + "번방 입장 실패(가득참).");
				obcm = new ChatMsg("SERVER", "444", "방이 가득 찼습니다.");

			} else {
				AppendText(UserName + " : " + RoomNumber + "번방 입장.");
				RoomCnt[RoomNumber]++;
				myRoomNumber = RoomNumber;
				
				/*
				for(int i = 0; i < 4; i++) {
					if(RoomUsers[RoomNumber][i].equals("")) {
						money[RoomNumber][i] = 5000000;
						RoomUsers[RoomNumber][i] = UserName;
						break;
					}
				}*/
				obcm = new ChatMsg("SERVER", "602", RoomNumber + " " + RoomCnt[RoomNumber]);
			}
			WriteAllObject(obcm);
		}

		// 게임방 퇴장 601
		public void RoomLogout(int RoomNumber) {
			RoomCnt[RoomNumber]--;
			myRoomNumber = 0;
			AppendText(UserName + " : " + RoomNumber + "번방 퇴장.");
			/*
			for(int i = 0; i < 4; i++) {
				if(RoomUsers[RoomNumber][i].equals(UserName)) {
					money[RoomNumber][i] = 5000000;
					RoomUsers[RoomNumber][i] = "";
					for(int j = 0; j < 22; j++) {
						if(land[RoomNumber][j]/10 == i*10)
							land[RoomNumber][j] = 0;
					}
					break;
				}
			}*/
			
			for (int i = 1; i <= 2; i++) {
				ChatMsg obcm = new ChatMsg(UserName, "602", i + " " + RoomCnt[i]);
				WriteAllObject(obcm);
			}
			
			ChatMsg obcm = new ChatMsg("SERVER", "602", RoomNumber + " " + RoomCnt[RoomNumber]);
			WriteAllObject(obcm);
		}

		// 게임 시작
		public void RoomStart(int RoomNumber) {
			RoomStatus[RoomNumber] = "게임중";
			for (int i = 0; i < 4; i++) { //방 인게임 데이터 초기화
				money[RoomNumber][i] = 5000000;	//	초기 마블
			}
			for (int i = 0; i < 22; i++) {
				land[RoomNumber][i] = 0;		//	모두 빈땅으로 설정
			}
			ChatMsg obcm = new ChatMsg("SERVER", "603", RoomNumber + " " + RoomStatus[RoomNumber]);
			WriteAllObject(obcm);
		}

		// 게임 종료
		public void RoomEnd(int RoomNumber) {
			RoomStatus[RoomNumber] = "대기중";

			ChatMsg obcm = new ChatMsg("SERVER", "603", RoomNumber + " " + RoomStatus[RoomNumber]);
			WriteAllObject(obcm);
		}

		// 모든 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteAll(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteOne(str);
			}
		}

		// 모든 User들에게 Object를 방송. 채팅 message와 image object를 보낼 수 있다
		public void WriteAllObject(ChatMsg obj) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteChatMsg(obj);
			}
		}

		// 나를 제외한 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteOthers(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user != this && user.UserStatus == "O")
					user.WriteOne(str);
			}
		}

		// Windows 처럼 message 제외한 나머지 부분은 NULL 로 만들기 위한 함수
		public byte[] MakePacket(String msg) {
			byte[] packet = new byte[BUF_LEN];
			byte[] bb = null;
			int i;
			for (i = 0; i < BUF_LEN; i++)
				packet[i] = 0;
			try {
				bb = msg.getBytes("euc-kr");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (i = 0; i < bb.length; i++)
				packet[i] = bb[i];
			return packet;
		}

		// UserService Thread가 담당하는 Client 에게 1:1 전송
		public void WriteOne(String msg) {
			ChatMsg obcm = new ChatMsg("SERVER", "200", msg);
			WriteChatMsg(obcm);
		}

		// 귓속말 전송
		public void WritePrivate(String msg) {
			ChatMsg obcm = new ChatMsg("귓속말", "200", msg);
			WriteChatMsg(obcm);
		}

		// 게임방에만 전송
		public void WriteRoom(String UserName, String protocol, String data, int RoomNumber) {
			ChatMsg obcm = new ChatMsg(UserName, protocol, data);
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.myRoomNumber == RoomNumber)
					user.WriteChatMsg(obcm);
			}
		}

		//
		public void WriteChatMsg(ChatMsg obj) {
			try {
				oos.writeObject(obj.code);
				oos.writeObject(obj.UserName);
				oos.writeObject(obj.data);
				if (obj.code.equals("300")) {
					oos.writeObject(obj.imgbytes);
					// oos.writeObject(obj.bimg);
				}
			} catch (IOException e) {
				AppendText("oos.writeObject(ob) error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Logout();

			}
		}

		public ChatMsg ReadChatMsg() {
			Object obj = null;
			String msg = null;
			ChatMsg cm = new ChatMsg("", "", "");
			// Android와 호환성을 위해 각각의 Field를 따로따로 읽는다.
			try {
				obj = ois.readObject();
				cm.code = (String) obj;
				obj = ois.readObject();
				cm.UserName = (String) obj;
				obj = ois.readObject();
				cm.data = (String) obj;
				if (cm.code.equals("300")) {
					obj = ois.readObject();
					cm.imgbytes = (byte[]) obj;
//					obj = ois.readObject();
//					cm.bimg = (BufferedImage) obj;
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				Logout();
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Logout();
				return null;
			}
			return cm;
		}

		public void run() {
			while (true) {
				ChatMsg cm = null;
				if (client_socket == null)
					break;
				cm = ReadChatMsg();
				if (cm == null)
					break;
				if (cm.code.length() == 0)
					break;
				AppendObject(cm);
				if (cm.code.matches("100")) {
					UserName = cm.UserName;
					UserStatus = "O"; // Online 상태
					Login();
				} else if (cm.code.matches("200")) {
					String msg = String.format("[%s] %s", cm.UserName, cm.data);
					AppendText(msg); // server 화면에 출력
					String[] args = msg.split(" "); // 단어들을 분리한다.
					if (args.length == 1) { // Enter key 만 들어온 경우 Wakeup 처리만 한다.
						UserStatus = "O";
					} else if (args[1].matches("/exit")) {
						Logout();
						break;
					} else if (args[1].matches("/list")) {
						WriteOne("User list\n");
						WriteOne("Name\tStatus\n");
						WriteOne("-----------------------------\n");
						for (int i = 0; i < user_vc.size(); i++) {
							UserService user = (UserService) user_vc.elementAt(i);
							WriteOne(user.UserName + "\t" + user.UserStatus + "\n");
						}
						WriteOne("-----------------------------\n");
					} else if (args[1].matches("/sleep")) {
						UserStatus = "S";
					} else if (args[1].matches("/wakeup")) {
						UserStatus = "O";
					} else if (args[1].matches("/to")) { // 귓속말
						for (int i = 0; i < user_vc.size(); i++) {
							UserService user = (UserService) user_vc.elementAt(i);
							if (user.UserName.matches(args[2]) && user.UserStatus.matches("O")) {
								String msg2 = "";
								for (int j = 3; j < args.length; j++) {// 실제 message 부분
									msg2 += args[j];
									if (j < args.length - 1)
										msg2 += " ";
								}
								// /to 빼고.. [귓속말] [user1] Hello user2..
								user.WritePrivate(args[0] + " " + msg2 + "\n");
								// user.WriteOne("[귓속말] " + args[0] + " " + msg2 + "\n");
								break;
							}
						}
					} else { // 일반 채팅 메시지
						UserStatus = "O";
						// WriteAll(msg + "\n"); // Write All
						WriteAllObject(cm);
					}
				} else if (cm.code.matches("400")) { // logout message 처리
					Logout();
					break;
				} else if (cm.code.matches("300")) {
					WriteAllObject(cm);
				} else if (cm.code.matches("600")) {
					RoomLogin(Integer.parseInt(cm.data));
				} else if (cm.code.matches("601")) {
					RoomLogout(Integer.parseInt(cm.data));
				} else if (cm.code.matches("603")) {
					String[] cmData = cm.data.split(" ");
					if (cmData[1].matches(".*대기중")) {
						RoomEnd(Integer.parseInt(cmData[0]));
					} else if (cmData[1].matches(".*게임중")) {
						RoomStart(Integer.parseInt(cmData[0]));
					}
				} else if(cm.code.matches("999")) {
					for (int i = 1; i <= 2; i++) {
						ChatMsg obcm = new ChatMsg(UserName, "602", i + " " + RoomCnt[i]);
						WriteAllObject(obcm);
					}
				}
			} // while
		} // run
	}

}
