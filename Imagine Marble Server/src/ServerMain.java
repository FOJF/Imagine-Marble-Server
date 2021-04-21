//JavaObjServer.java ObjectStream 기반 채팅 Server

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

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
	private int[] RoomCnt = new int[5]; // 0은 방X 1부터 1번방 4번까지 방들의 인원수

	private int[] RoomStatus = new int[4];

	private int[] landPrice = { 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000,
			50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000, 50000,
			50000, 50000, 50000 };
	private int[] otherLandPrice = { 150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000,
			150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000, 150000 };
	private int[] tollFee = { 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000,
			100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000 };

	// 인게임 데이터
	private int[][] money = new int[4][4];
	private int[][] landOwner = new int[4][22]; // 땅 주인의 유저인덱스
	private String[][] RoomUsers = new String[4][4];
	private boolean[][] UserReady = new boolean[4][4];
	private int[][] UserPos = new int[4][4];

	private int[] TurnCheck = new int[4];
	private int[] UserTurn = new int[4];
	private int[] BeforeUserTurn = new int[4];
	private Random random = new Random();

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

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				RoomUsers[i][j] = "";
			}
		}

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
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	public void AppendObject(ChatMsg msg) {
		textArea.append("code = " + msg.code + "\n");
		textArea.append("id = " + msg.UserName + "\n");
		textArea.append("data = " + msg.data + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	// User 당 생성되는 Thread
	// Read One 에서 대기 -> Write All
	class UserService extends Thread {

		private ObjectInputStream ois;
		private ObjectOutputStream oos;

		private Socket client_socket;
		private Vector user_vc;
		public String UserName = "";
		public String UserStatus;
		public int myRoomNumber;

		public UserService(Socket client_socket) {
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

			for (int i = 1; i < 5; i++) { // 첫 로그인시 방번
				ChatMsg obcm = new ChatMsg(UserName, "602", i + " " + RoomCnt[i]);
				WriteChatMsg(obcm);
				obcm = new ChatMsg("SERVER", "603", i + " " + RoomStatus[i - 1]);
				WriteChatMsg(obcm);
			}
		}

		public void Logout() {
			String msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
			UserVec.removeElement(this); // Logout한 현재 객체를 벡터에서 지운다
			WriteAll(msg); // 나를 제외한 다른 User들에게 전송
			this.client_socket = null;
			AppendText("사용자 " + "[" + UserName + "] 퇴장. 현재 참가자 수 " + UserVec.size());
		}

		public void Ready(int RoomNumber) {
			int ReadyCnt = 0;
			for (int i = 0; i < 4; i++) {
				if (RoomUsers[RoomNumber - 1][i].equals(UserName)) {
					UserReady[RoomNumber - 1][i] = true;
					AppendText(RoomNumber + "번방 : " + UserName + "님이 준비완료.");
					WriteRoom("SERVER", "605", i + "", RoomNumber);
				}
				if (UserReady[RoomNumber - 1][i] == true)
					ReadyCnt++;
			}
			if (ReadyCnt == RoomCnt[RoomNumber] && RoomCnt[RoomNumber] > 1)
				RoomStart(RoomNumber);
		}

		public void UnReady(int RoomNumber) {
			for (int i = 0; i < 4; i++) {
				if (RoomUsers[RoomNumber - 1][i].equals(UserName)) {
					UserReady[RoomNumber - 1][i] = false;
					WriteRoom("SERVER", "606", i + "", RoomNumber);
				}
			}
		}

		// 게임방 입장 600(게임방 데이터 전송)
		public void RoomLogin(int RoomNumber) {
			ChatMsg obcm;
			if (RoomStatus[RoomNumber - 1] > 0) {
				AppendText(UserName + " : " + RoomNumber + "번방 입장 실패(게임중).");
				obcm = new ChatMsg("SERVER", "444", "게임이 이미 진행 중입니다.");
			} else if (RoomCnt[RoomNumber] == 4) {
				AppendText(UserName + " : " + RoomNumber + "번방 입장 실패(가득참).");
				obcm = new ChatMsg("SERVER", "444", "방이 가득 찼습니다.");
			} else {
				AppendText(UserName + " : " + RoomNumber + "번방 입장.");
				RoomCnt[RoomNumber]++;
				myRoomNumber = RoomNumber;

				for (int i = 0; i < 4; i++) {
					if (UserReady[myRoomNumber - 1][i] == true) {
						WriteRoom("SERVER", "605", i + "", RoomNumber);
					} else {
						WriteRoom("SERVER", "606", i + "", RoomNumber);
					}

					if (RoomUsers[RoomNumber - 1][i].equals("")) {
						RoomUsers[RoomNumber - 1][i] = UserName;
						break;
					}
				}

				for (int i = 0; i < 4; i++) {
					WriteRoom("SERVER", "604", i + " " + RoomUsers[RoomNumber - 1][i], RoomNumber);
				}

				obcm = new ChatMsg("SERVER", "602", RoomNumber + " " + RoomCnt[RoomNumber]);
			}
			WriteAllObject(obcm);
		}

		// 게임방 퇴장 601
		public void RoomLogout(int RoomNumber) {
			RoomCnt[RoomNumber]--;
			myRoomNumber = 0;
			AppendText(UserName + " : " + RoomNumber + "번방 퇴장.");

			for (int i = 0; i < 4; i++) { // 퇴장한 유저의 데이터 초기화
				if (RoomUsers[RoomNumber - 1][i].equals(UserName)) {
					RoomUsers[RoomNumber - 1][i] = "";
					UserReady[RoomNumber - 1][i] = false;
					WriteRoom("SERVER", "606", i + "", RoomNumber);
					break;
				}
			}

			for (int i = 0; i < 4; i++) {
				WriteRoom("SERVER", "604", i + " " + RoomUsers[RoomNumber - 1][i], RoomNumber);
			}

			int ReadyCnt = 0;
			for (int i = 0; i < 4; i++) {
				if (UserReady[RoomNumber - 1][i] == true)
					ReadyCnt++;
			}
			if (ReadyCnt == RoomCnt[RoomNumber] && RoomCnt[RoomNumber] > 1)
				RoomStart(RoomNumber);

			/*
			 * for(int i = 0; i < 4; i++) { if(RoomUsers[RoomNumber][i].equals(UserName)) {
			 * money[RoomNumber][i] = 5000000; RoomUsers[RoomNumber][i] = ""; for(int j = 0;
			 * j < 22; j++) { if(land[RoomNumber][j]/10 == i*10) land[RoomNumber][j] = 0; }
			 * break; } }
			 */

			ChatMsg obcm = new ChatMsg("SERVER", "602", RoomNumber + " " + RoomCnt[RoomNumber]);
			WriteAllObject(obcm);
		}

		// 게임 시작
		public void RoomStart(int RoomNumber) {
			if (RoomStatus[RoomNumber - 1] == 0) {
				RoomStatus[RoomNumber - 1] = 1;
				for (int i = 0; i < 4; i++) { // 방 인게임 데이터 초기화
					if (!RoomUsers[RoomNumber - 1][i].equals("")) {

						money[RoomNumber - 1][i] = 200000; // 초기 마블
						WriteRoom("SERVER", "607", i + " " + money[RoomNumber - 1][i], RoomNumber);
						UserReady[RoomNumber - 1][i] = false;
						WriteRoom("SERVER", "606", i + "", RoomNumber);
					}
				}

				for (int i = 0; i < 22; i++) {
					landOwner[RoomNumber - 1][i] = 5; // 모두 빈땅으로 설정
					ChangeOwner(5, i);
				}

				for (int i = 0; i < 4; i++) { // 주사위 던지 유저 지정
					if (!RoomUsers[RoomNumber - 1][i].equals("")) {
						UserTurn[RoomNumber - 1] = i;
						TurnCheck[myRoomNumber - 1] = i;
						WriteRoom("SERVER", "608", RoomUsers[RoomNumber - 1][i], RoomNumber);
						break;
					}
				}

				AppendText(RoomNumber + "번방 게임시작!");
				ChatMsg obcm = new ChatMsg("SERVER", "603", RoomNumber + " " + RoomStatus[RoomNumber - 1]);
				WriteAllObject(obcm);
			}
		}

		// 게임 종료
		public void RoomEnd(int RoomNumber) {
			RoomStatus[RoomNumber - 1] = 0;

			for (int i = 0; i < 4; i++) {
				money[RoomNumber - 1][i] = 0;
				WriteRoom("SERVER", "607", i + " " + money[RoomNumber - 1][i], RoomNumber);
				UserPos[RoomNumber - 1][i] = 0;
			}

			TurnCheck[RoomNumber - 1] = 0;
			UserTurn[RoomNumber - 1] = 0;
			BeforeUserTurn[RoomNumber - 1] = 0;
			WriteRoom("SERVER", "608", "", RoomNumber);

			for (int i = 0; i < 22; i++) {
				landOwner[RoomNumber - 1][i] = 5; // 모두 빈땅으로 설정
				ChangeOwner(5, i);
			}

			ChatMsg obcm = new ChatMsg("SERVER", "603", RoomNumber + " " + RoomStatus[RoomNumber - 1]);
			WriteAllObject(obcm);
		}

		public void Roll(String data) {
			int[] Dices = new int[2];
			boolean noMoney = false;
			boolean isPlay = true;

			WriteRoom("SERVER", "608", "", myRoomNumber);
			for (int i = 0; i < Dices.length; i++) {
				Dices[i] = random.nextInt(6) + 1;
			}

			WriteRoom("SERVER", "609", Dices[0] + " " + Dices[1], myRoomNumber);

			int DiceSum = Dices[0] + Dices[1];

			UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] += DiceSum;
			if (UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] >= 22) { // 시작 지점 통과
				money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] += 50000;
				WriteRoom("SERVER", "607",
						UserTurn[myRoomNumber - 1] + " " + money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]],
						myRoomNumber);
			}
			UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] %= 22;
			WriteRoom("SERVER", "610",
					UserTurn[myRoomNumber - 1] + " " + UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]],
					myRoomNumber); // 말 이동
			if (UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] == 0) { // 시작점

			} else if (UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] == 2) { // 팀프로젝트
				money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] -= 50000;
				WriteRoom("SERVER", "607",
						UserTurn[myRoomNumber - 1] + " " + money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]],
						myRoomNumber);
			} else if (UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] == 4) { // 중간고사
				money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] -= 100000;
				WriteRoom("SERVER", "607",
						UserTurn[myRoomNumber - 1] + " " + money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]],
						myRoomNumber);
			} else if (UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] == 11) { // 대동제
				money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] += 100000;
				WriteRoom("SERVER", "607",
						UserTurn[myRoomNumber - 1] + " " + money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]],
						myRoomNumber);

			} else if (UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] == 15) { // 기말고사
				money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] -= 100000;
				WriteRoom("SERVER", "607",
						UserTurn[myRoomNumber - 1] + " " + money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]],
						myRoomNumber);

			} else {

				// 남의 땅일때
				if (landOwner[myRoomNumber
						- 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]] != UserTurn[myRoomNumber - 1]
						&& landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]] != 5) {

					// 주사위를 굴려 이동한 유저의 돈 감소
					money[myRoomNumber - 1][UserTurn[myRoomNumber
							- 1]] -= tollFee[UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]];

					// 땅 주인의 돈 증가
					money[myRoomNumber - 1][landOwner[myRoomNumber
							- 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]]] += tollFee[UserPos[myRoomNumber
									- 1][UserTurn[myRoomNumber - 1]]];

					// 유저의 돈 변화 클라이언트로 전송
					WriteRoom("SERVER", "607",
							UserTurn[myRoomNumber - 1] + " " + money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]],
							myRoomNumber);
					WriteRoom("SERVER", "607",
							landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]] + " "
									+ money[myRoomNumber - 1][landOwner[myRoomNumber
											- 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]]],
							myRoomNumber); // 땅 주인의 돈 변화
				}

				// 파산;
				if (money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] <= 0) {
					for (int i = 0; i < landOwner[myRoomNumber - 1].length; i++) {
						if (landOwner[myRoomNumber - 1][i] == UserTurn[myRoomNumber - 1]) {
							landOwner[myRoomNumber - 1][i] = 5;
							ChangeOwner(5, i);
						}
					}
					noMoney = true;
				} else // 파산이 아닐 경우
				if ((landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]] == 5
						&& money[myRoomNumber - 1][UserTurn[myRoomNumber
								- 1]] > landPrice[UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]])
						|| (landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]] != 5
								&& money[myRoomNumber - 1][UserTurn[myRoomNumber
										- 1]] > otherLandPrice[UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]]))
					WriteRoom("SERVER", "611", UserTurn[myRoomNumber - 1] + " "
							+ landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][UserTurn[myRoomNumber - 1]]],
							myRoomNumber);

			}

			int cnt = 0;
			int winUser = 5;
			for (int i = 0; i < 4; i++) {
				if (money[myRoomNumber - 1][i] > 0) {
					cnt++;
					winUser = i;
				}
			}

			if (cnt == 1) {
				isPlay = false;
				WriteRoom("SERVER", "613", RoomUsers[myRoomNumber - 1][winUser], myRoomNumber);
				RoomEnd(myRoomNumber);

			}
			if (isPlay)
				NextTurn(noMoney);
		}

		public void BuyLand() {
			// 땅 주인의 돈 증가
			if (landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]]] != 5) {

				// 땅 주인의 돈 변화
				money[myRoomNumber
						- 1][landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber
								- 1]]]] += otherLandPrice[UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]]];

				WriteRoom("SERVER", "607",
						landOwner[myRoomNumber - 1][UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]]] + " "
								+ money[myRoomNumber - 1][landOwner[myRoomNumber
										- 1][UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]]]],
						myRoomNumber);

				// 주사위를 굴려 이동한 유저의 돈 감소
				money[myRoomNumber - 1][BeforeUserTurn[myRoomNumber
						- 1]] -= otherLandPrice[UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]]];
				// 현재 턴의 유저의 돈 변화
				WriteRoom("SERVER", "607", BeforeUserTurn[myRoomNumber - 1] + " "
						+ money[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]], myRoomNumber);
			} else {
				// 주사위를 굴려 이동한 유저의 돈 감소
				money[myRoomNumber - 1][BeforeUserTurn[myRoomNumber
						- 1]] -= landPrice[UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]]];

				// 현재 턴의 유저의 돈 변화
				WriteRoom("SERVER", "607", BeforeUserTurn[myRoomNumber - 1] + " "
						+ money[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]], myRoomNumber);
			}
			ChangeOwner(BeforeUserTurn[myRoomNumber - 1], UserPos[myRoomNumber - 1][BeforeUserTurn[myRoomNumber - 1]]);
		}

		public void ChangeOwner(int buyUser, int landIndex) {
			landOwner[myRoomNumber - 1][landIndex] = buyUser;

			WriteRoom("SERVER", "612", landOwner[myRoomNumber - 1][landIndex] + " " + landIndex, myRoomNumber);
		}

		public void NextTurn(boolean noMoney) {

			BeforeUserTurn[myRoomNumber - 1] = UserTurn[myRoomNumber - 1];
			while (true) { // 다음 유저에게 주사위 굴리기 권한 부여
				UserTurn[myRoomNumber - 1]++;
				UserTurn[myRoomNumber - 1] %= 4;

				if (!RoomUsers[myRoomNumber - 1][UserTurn[myRoomNumber - 1]].equals("")
						&& money[myRoomNumber - 1][UserTurn[myRoomNumber - 1]] > 0) {
					WriteRoom("SERVER", "608", RoomUsers[myRoomNumber - 1][UserTurn[myRoomNumber - 1]], myRoomNumber);

					if (noMoney)
						TurnCheck[myRoomNumber - 1] = UserTurn[myRoomNumber - 1];

					if (TurnCheck[myRoomNumber - 1] == UserTurn[myRoomNumber - 1]) {
						RoomStatus[myRoomNumber - 1]++;
						AppendText(myRoomNumber + "번방 - " + RoomStatus[myRoomNumber - 1] + "턴 진행중");
						ChatMsg obcm = new ChatMsg("SERVER", "603", myRoomNumber + " " + RoomStatus[myRoomNumber - 1]);
						WriteAllObject(obcm);
					}

					break;
				}

			}
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
		public void WriteRoom(String UserName, String code, String data, int RoomNumber) {
			ChatMsg obcm = new ChatMsg(UserName, code, data);
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
			ChatMsg cm = new ChatMsg("", "", "");

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
				}
			} catch (ClassNotFoundException e) {
				Logout();
				e.printStackTrace();
				return null;
			} catch (IOException e) {
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
					if (cm.UserName.equals("")) {
						ChatMsg obcm = new ChatMsg("SERVER", "444", "닉네임 공백");
						WriteChatMsg(obcm);
					} else {
						int i;
						for (i = 0; i < user_vc.size(); i++) {
							UserService user = (UserService) user_vc.elementAt(i);
							if (user.UserName.equals(cm.UserName)) {
								ChatMsg obcm = new ChatMsg("SERVER", "444", "닉네임 중복");
								WriteChatMsg(obcm);
								break;
							}
						}

						if (i == user_vc.size()) {
							UserName = cm.UserName;
							UserStatus = "O"; // Online 상태
							Login();
						}
					}
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
								user.WritePrivate(args[0] + " " + msg2 + "\n");
								break;
							}
						}
					} else { // 일반 채팅 메시지
						UserStatus = "O";
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
				} else if (cm.code.matches("603")) {// 603 방번호 start,end
					String[] cmData = cm.data.split(" ");
					if (cmData[1].equals("end")) {
						RoomEnd(Integer.parseInt(cmData[0]));
					} else if (cmData[1].equals("start")) {
						RoomStart(Integer.parseInt(cmData[0]));
					}
				} else if (cm.code.matches("605")) {
					Ready(Integer.parseInt(cm.data));
				} else if (cm.code.matches("606")) {
					UnReady(Integer.parseInt(cm.data));
				} else if (cm.code.matches("609")) {
					Roll(cm.data);
				} else if (cm.code.matches("612")) {
					BuyLand();
				}
			} // while
		} // run
	}

}
