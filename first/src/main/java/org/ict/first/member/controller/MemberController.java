package org.ict.first.member.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ict.first.common.SearchDate;
import org.ict.first.common.Searchs;
import org.ict.first.member.model.service.MemberService;
import org.ict.first.member.model.vo.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

//xml에 클래스를 controller 로 자동 등록하기 위해
//spring은 controller인것을 annotation해야함. 
@Controller
public class MemberController {
	// 이 컨트롤러 안의 메소드들이 구동되었는지 확인하는 로그를 출력하기 위한 로그객체 생성
	private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

	@Autowired // 자동 의존성주입(DI) : 자동 객체 생성됨
	private MemberService memberService;

	// 웹 서비스 요청 하나당 메소드 하나씩 작성하는 방식임 *****
	// 1. 뷰 페이지 이동 처리용 메소드 : 거의 Get 방식 사용 ------------------------------

	// 로그인 페이지 내보내기용 메소드
	// controller의 리턴타입은 void, String, ??
	@RequestMapping(value = "loginPage.do", method = { RequestMethod.GET, RequestMethod.POST })
	public String moveLoginPage() {
		return "member/loginPage";
	}
	
	//회원가입 페이지(enrollPage.do) 내보내기용
	//메소드 리턴타입은 보통 2가지 : String, ModelAndView(view)
	@RequestMapping("enrollPage.do")
	public String moveEnrollPage() {
		return "member/enrollPage";
	}

	
	//회원정보 수정페이지(moveup.do) 내보내기용
	//mypage를 수정페이지로 지정해도 됨
	@RequestMapping("moveup.do")
	public ModelAndView moveUpdatePage(@RequestParam("userid") String userid, ModelAndView mv) {
		Member member = memberService.selectMember(userid);
		
		if(member != null) {	//회원정보 조회 성공했다면,
			mv.addObject("member", member);
			mv.setViewName("member/updatePage");
	
		}else {
			mv.addObject("message", userid + " : 회원정보 조회 실패!");
			mv.setViewName("common/error");
		}
		return mv;
	}
		
	
	
	
	// 2. 서비스와 연결되는 요청 처리용 메소드 ----------------------------------------
	/*
	 * login 처리용 메소드 : Servlet 방식으로 처리
	 * 
	 * @RequestMapping(value="login.do", method=RequestMethod.POST) public String
	 * loginMethod(HttpServletRequest request, HttpServletResponse response, Model
	 * model ) { //1. 전송 온 값 꺼내기 String userid = request.getParameter("userid");
	 * String userpwd = request.getParameter("userpwd"); Member member = new
	 * Member(); member.setUserid(request.getParameter("userid"));
	 * member.setUserpwd(request.getParameter("userpwd"));
	 * 
	 * //2. 서비스 모델로 전송하고 결과 받기 Member loginMember =
	 * memberService.selectLogin(member);
	 * 
	 * //3. 받은 결과를 가지고 성공/실패 페이지를 선택하여 리턴 if(loginMember != null) { //세션 객체 생성
	 * HttpSession session = request.getSession();
	 * session.setAttribute("loginMember", loginMember); return "common/main"; }else
	 * { model.addAttribute("message", "로그인 실패"); return "common/error"; }
	 * 
	 * }
	 */

	// login 처리용 메소드
	// framework 방식 : command 객체 사용
	// 서버로 전송 온 parameter 값을 저장하는 객체를 command 객체라고 함.
	// input tag의 name과 vo 객체의 필드명이 같으면 command 객체가 생성된다.
	@RequestMapping(value="login.do", method=RequestMethod.POST)
	public String loginMethod(Member member, HttpSession session, SessionStatus status, Model model) {	//RequestDispatcher = model
		//login info check
		logger.info("login.do : " + member);
		
		//서비스 모델로 전달하고 결과받기
		Member loginMember = memberService.selectLogin(member);
		
		if(loginMember != null) {
			session.setAttribute("loginMember", loginMember);
			
			status.setComplete(); 	//요청을 받은 것에 대한 결과를 리턴 : login 요청 성공 = 200 전송
			return "common/main";
		}else {
			//RequestDispatcher = model
			model.addAttribute("message", "로그인 실패 : 아이디 혹은 암호를 확인하세요.<br>"
					+ "또는 로그인 제한된 회원인지 관리자에게 문의하세요.");
			return "common/error";
		}
	}

	@RequestMapping(value = "logout.do")	//value 생략가능 = ("logout.do")
	public String logoutMethod(HttpServletRequest request, Model model) {	//HttpServletRequest : getsession을 위해
		HttpSession session = request.getSession(false);	//있으면 가져오고, 없으면 null 리턴하도록 false로 설정
		
		//logout info check
		logger.info("logout.do : " + session);
		
		if(session != null) {
			session.invalidate();
			return "common/main";
		}else{	//자동로그아웃된 경우
			model.addAttribute("message", "로그인 세션이 존재하지 않습니다.");	//model에는 리퀘스트랑 리스판스를 가지고 있어 자동 포워드됨.
			return "common/error";
		}

	}
	
	//ajax 통신으로 아이디 중복확인 요청 처리용 메소드
	@RequestMapping(value = "idchk.do", method = RequestMethod.POST)
	public void dupCheckIdMethod(
			@RequestParam("userid") String userid, HttpServletResponse response) throws IOException { //리턴이 되면, view리졸뷰에게 감. 그럼 새로고침됨. ajax는 새로고침되면 안됨. 그래서 void 사용
	//매개변수자리에 올수 있는 어노테이션이 있음. 컨트롤러에서 파라미터 값을 넘겨받아 변수에 담아서 보낼 수 있음. 
		//=> @RequestParam("userid") String userid
		
		int idCount = memberService.selectDupCheckId(userid);
		
		String returnStr = null;
		if(idCount == 0) {
			returnStr = "ok";
		}else {
			returnStr = "duple";
		}
		
		//response를 이용하여 클라이언트와 출력스트림 연결 후 값 전송
			//1. contentType 설정
		response.setContentType("text/html; charset=utf-8");
		
			//2. 출력스트림 연결
		PrintWriter out = response.getWriter();
		
			//3. 값 전송
			//적을때 : append / 많을때 : write?, ??
		out.append(returnStr);
		out.flush();
		out.close();
	}
	
	//회원가입(enroll.do) 요청 처리용 메소드
	@RequestMapping(value ="enroll.do", method = RequestMethod.POST)
	public String memberInsertMethod(Member member, Model model) {	//error 처리용 Model
		//check
		logger.info("enroll.do : " + member);
		
		if(memberService.insertMember(member) > 0) {
			//enroll success
			return "common/main";
		}else {
			//enroll fail
			model.addAttribute("message", "회원 가입 실패!");
			return "common/error";
		}
	}
	
	
	//마이페이지 클릭시 내 정보 보기 요청 처리용 메소드
	//리턴타입은 String, ModelAndView 사용 가능
	@RequestMapping("myinfo.do")
	public ModelAndView memberDetailMethod(@RequestParam("userid") String userid, ModelAndView mv) {
		//서비스로 아이디 전달, 해당 회원 정보 받기
		Member member = memberService.selectMember(userid);
		
		//check
		logger.info("myinfo.do : " + userid);
		
		if(member != null) {
			mv.addObject("member", member); 	
			//Model 또는 ModelAndView 에 저장하는 것은
			// == request.setAttribute("member", member);
			mv.setViewName("member/myinfoPage");
		}else {
			mv.addObject("message", userid + " : 회원 정보 조회 실패!");
			mv.setViewName("common/error");
		}
		
		return mv;
	}

	
	//회원 탈퇴(삭제) 요청 처리용
	//탈퇴시 자동 로그아웃
	@RequestMapping("mdel.do")
	public String memberDeleteMethod(@RequestParam("userid") String userid, Model model) {
		//check
		logger.info("mdel.do : " + userid);
		
		if(memberService.deleteMember(userid) > 0) {
			//회원탈퇴 성공시, 자동 로그아웃 처리
			//컨트롤러 메소드에서 다른 [컨트롤러] 메소드 호출할 수 있음 => redirect: .do
			return "redirect:logout.do";
		}else {
			model.addAttribute("message", userid + " : 회원 탈퇴 실패!");
			return "common/error";
		}
	}
	
	//회원정보 수정 처리용 : 수정 성공시 myinfoPage.jsp 로 이동
	@RequestMapping(value="mupdate.do", method = RequestMethod.POST)
	public String memberUpdateMethod(Member member, Model model) {
		logger.info("mupdate.do : " + member);
		
		if(memberService.updateMember(member) > 0) {	//처리된 행의 갯수가 1개이상이냐
			//수정 성공시, 컨트롤러의 메소드를 직접 호출 처리
			//필요시, 값을 전달 가능 : 쿼리스트링 사용.
			//queryString : ?name=value&name=value
			return "redirect:myinfo.do?userid=" + member.getUserid();
		} else {
			model.addAttribute("message", member.getUserid() + " : 회원 정보 수정 실패!");
			return "common/error";
		}
	}
	
	//회원관리용 회원전체목록 조회 처리용
	@RequestMapping("mlist.do")
	public ModelAndView memberListViewMethod(ModelAndView mv) {
		ArrayList<Member> list = memberService.selectList();
		
		if(list != null && list.size() > 0) {	//조회된 정보가 있다면
			mv.addObject("list", list);
			mv.setViewName("member/memberListView");
		}else {
			mv.addObject("message", "회원 정보가 존재하지 않습니다.");
			mv.setViewName("common/error");
		}
		
		return mv;
	}
	
	//관리자 기능 : 회원 로그인 제한/가능 처리용 메소드
	@RequestMapping("loginok.do")
	public ModelAndView changeLoginOkMethod(Member member, ModelAndView mv) {
		logger.info("loginok.do : " + member.getUserid() + ", " + member.getLogin_ok());
		if(memberService.updateLoginok(member) > 0) {	//수정 성공시 
			mv.setViewName("redirect:mlist.do");
		}else {
			mv.addObject("message", "로그인 제한/허용 처리 오류 발생!");
			mv.setViewName("common/error");
		}
		
		return mv;
	}
	
	//회원 검색 처리용
	@RequestMapping(value="msearch.do", method = RequestMethod.POST)
	public ModelAndView memberSearchMethod(HttpServletRequest request, ModelAndView mv) {
		//전송 온 값 꺼내기 ( 종류가 달라 직접꺼내기로)
		String action = request.getParameter("action");
		String keyword = null, beginDate = null, endDate = null;
		
		if(action.equals("enroll")) {	//enroll만 name이 keyword가 아님 begin, end로 옴
			beginDate = request.getParameter("begin");
			endDate = request.getParameter("end");
		}else {
			keyword = request.getParameter("keyword");
		}
		
		//서비스 메소드가 리턴하는 값 받을 리스트 준비
		ArrayList<Member> list = null;
		Searchs searchs = new Searchs();
		
		switch(action) {
		case "id" : 	searchs.setKeyword(keyword);
						list = memberService.selectSearchUserid(searchs); break;
		case "gender" : searchs.setKeyword(keyword);
						list = memberService.selectSearchGender(searchs); break;
		case "age" :  	searchs.setAge(Integer.parseInt(keyword));
						list = memberService.selectSearchAge(searchs); break;
		case "enroll" : list = memberService.selectSearchEnrollDate(new SearchDate(Date.valueOf(beginDate), Date.valueOf(endDate)));
						break;
		case "login" :  searchs.setKeyword(keyword);
						list = memberService.selectSearchLoginOK(searchs); break;
		}
		
		if(list != null && list.size() > 0) {
		mv.addObject("list", list);
		mv.setViewName("member/memberListView");
		}else {
			mv.addObject("message", action + " 검색에 대한 결과가 존재하지 않습니다.");
			mv.setViewName("common/error");
		}
		return mv;
	}
}

	