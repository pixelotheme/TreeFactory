package com.treefactory.board.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.treefactory.board.service.BoardDeleteService;
import com.treefactory.board.service.BoardListService;
import com.treefactory.board.service.BoardUpdateService;
import com.treefactory.board.service.BoardViewService;
import com.treefactory.board.service.BoardWriteService;
import com.treefactory.board.vo.BoardVO;
import com.treefactory.main.Controller;
import com.treefactory.main.Execute;
import com.treefactory.reply.service.ReplyListService;
import com.treefactory.reply.vo.ReplyVO;
import com.treefactory.util.CategoryPageObject;
import com.treefactory.util.ReplyPageObject;
import com.webjjang.util.PageObject;

public class BoardController implements Controller {

	//서버가 시작이 될 때 객체가 초기화 되어야만 한다 - DispacherServlet.init() 처리
	private BoardListService boardListService;
	private BoardViewService boardViewService;
	private BoardWriteService boardWriteService;
	private BoardUpdateService boardUpdateService;
	private BoardDeleteService boardDeleteService;
	
	//댓글
	private ReplyListService replyListService;
	
	public void setBoardListService(BoardListService boardListService) {
		this.boardListService = boardListService;
	}

	public void setBoardViewService(BoardViewService boardViewService) {
		this.boardViewService = boardViewService;
	}

	public void setBoardWriteService(BoardWriteService boardWriteService) {
		this.boardWriteService = boardWriteService;
	}

	public void setBoardUpdateService(BoardUpdateService boardUpdateService) {
		this.boardUpdateService = boardUpdateService;
	}

	public void setBoardDeleteService(BoardDeleteService boardDeleteService) {
		this.boardDeleteService = boardDeleteService;
	}

	public void setReplyListService(ReplyListService replyListService) {
		this.replyListService = replyListService;
	}
	
	//request를 전달받아 처리한다, 
	//Strirg 은 JSP 에 대한 정보(어떤JSP를 쓸것인지)를 담고있다, URL 이동 정보를 담고있다("redirect:url" 형식 사용)
	@Override
	public String execute(HttpServletRequest request) throws Exception{
		
		System.out.println("BoardController.execute()- 게싶판 처리하고 있다");
		
		String jsp = null;
		
		//uri - /board/실행서비스.do  - 처리 service 결정하는 - /list.do
		String uri = request.getServletPath();
		
		//두번째 슬레시부터 글자 찾기 - switch 문으로 해당되는것 실행되게끔 가져오는 처리
		String serviceUri = uri.substring(uri.indexOf("/", 1));
		System.out.println("BoardController.execute().serviceUri - "+serviceUri);
		
		switch (serviceUri) {
		//일반게시판 리스트
		case "/list.do":
			//page정보 받기
			CategoryPageObject categoryPageObject =  CategoryPageObject.getInstance(request);
			//jsp 자바단에 쓰던 실행 메서드 (excute 메서드 를 사용해도 되지만 spring에서 약간 달라져서 보류)
			
			String strCategory = request.getParameter("category");
			if(strCategory == null || strCategory.equals("")) {
				categoryPageObject.setCategory("all");
			}
			else{categoryPageObject.setCategory(strCategory);
			}
			
			request.setAttribute("list", Execute.service(boardListService, categoryPageObject));
			request.setAttribute("categoryPageObject", categoryPageObject);
			
			//redirect: -> url 이동 , 없으면 jsp 로 이동
			//url 에 추가로 값이 안넘어가도 되니까 jsp 로 보낸듯?
			jsp = "board/list";
			break;
			//일반게시판 글쓰기 폼
		case "/writeForm.do":
			
			jsp = "board/writeForm";
			break;
			//일반 게시판 글쓰기 처리
		case "/write.do":
			
			//주소창에 넘어오는 데이터받기

			String title = request.getParameter("title");
			String content = request.getParameter("content");
			String id = request.getParameter("writer");

			String strPerPageNum = request.getParameter("perPageNum");
			
			//넘겨받은 데이터를 vo로 생성해서 넣어준다.
			BoardVO vo = new BoardVO();
			vo.setTitle(title);
			vo.setContent(content);
			vo.setId(id);

			//db등록 / 이미 등록되어있는 service 가져와서 쓴다
			Execute.service(boardWriteService, vo);
			//이동
//			response.sendRedirect("list.jsp?perPageNum"+strPerPageNum);
			//redirect: - url 이동 , 없으면 jsp 로 이동
			jsp = "redirect:list.do?perPageNum="+strPerPageNum;
			break;

		case "/view.do":
			String noStr = request.getParameter("no");
			long no = Long.parseLong(noStr);

			String strInc = request.getParameter("inc");
			int inc = Integer.parseInt(strInc);

			//request는 요청하는 것이 모두 담겨있음 
			//"location='view.jsp?no=1'"
			//전달 받은 데이터 받기 /무조건 문자열로받아야함 / no 라는 키로 넘긴다(주소창에 나옴)
			//게시판에대한 페이지,검색정보
			PageObject pageObject = PageObject.getInstance(request);

			//댓글 페이지 정보 가져오기
			ReplyPageObject replyPageObject = new ReplyPageObject();
			String strReplyPage = request.getParameter("replyPage");
			//값이 있으면 replyPage를 넣는다
			if (strReplyPage != null)
				replyPageObject.setPage(Long.parseLong(strReplyPage));

			//글번호 받아오기
			System.out.println("글번호"+no);
			replyPageObject.setNo(no);
			//한 페이지당 보여주는 데이터의 갯수는 기본 값인 10을 그대로 사용한다.

			//게시판 글보기 데이터 가져오기
			vo = (BoardVO) Execute.service(boardViewService, new Object[]{no, inc});
			//게시판 댓글 리스트 데이터 가져오기 - board/view.jsp - com.webjjang.reply.service.ReplyListService -> dao.ReplyDAO
			@SuppressWarnings("unchecked") 
			List<ReplyVO> list = (List<ReplyVO>) Execute.service(replyListService, replyPageObject);

			//vo.content를 그대로 보여 주면 줄바꿈 무시, 여러개의 공백문자 무시 ->처리해준다 replace
			vo.setContent(vo.getContent().replace("\n", "<br>").replace(" ", "&nbsp;"));

			//강제 오류 발생
			// System.out.println(10/0);

			//EL 객체를 이용해서 데이터 표시하고자 한다면 JSP 저장 기본 객체 중 하나에 저장이 되어 있어야만 한다.
			//application(서버실행시) or session(상용자가 요청시생김) or request(데이터를뿌려줄때만 쓰고 버린다)  or pagecontext(해당하는jsp에서만 쓴다)
			request.setAttribute("vo", vo);
			request.setAttribute("pageObject", pageObject);
			request.setAttribute("replyPageObject", replyPageObject);
			request.setAttribute("list", list);
			
			jsp = "board/view";
			break;
			
			case "/updateForm.do":
			    pageObject = PageObject.getInstance(request);
			    
			    noStr = request.getParameter("no");
			    no = Long.parseLong(noStr);

			    // BoardViewService service = new BoardViewService();
			    //조회수 증가x
			    // BoardVO vo = service.service(no, 0);

			    //EL 객체를 이용해서 데이터 표시하고자 한다면 JSP 저장 기본 객체 중 하나에 저장이 되어 있어야만 한다.
			    //application(서버실행시) or session(상용자가 요청시생김) or request(데이터를뿌려줄때만 쓰고 버린다)  or pagecontext(해당하는jsp에서만 쓴다)
			    request.setAttribute("pageObject", pageObject);
			    request.setAttribute("vo", Execute.service(boardViewService, new Object[]{no, 0}));
				
			    //updateForm.jsp 로 이동시키기 위해 정보 저장
			    jsp = "board/updateForm";
				break;
				
			case "/update.do":
				String strNo = request.getParameter("no");
				no = Long.parseLong(strNo);


				title = request.getParameter("title");
				content = request.getParameter("content");
				id = request.getParameter("id");

				//페이지 검색 정보 받기
				pageObject = PageObject.getInstance(request);

				vo = new BoardVO();
				vo.setNo(no);
				vo.setTitle(title);
				vo.setContent(content);
				vo.setId(id);

				//jsp(=이전controller역할) -> BoardUpdateService -> BoardDAO
				// BoardUpdateService service = new BoardUpdateService();
				// int result = service.service(vo);
				int result = (Integer)Execute.service(boardUpdateService, vo);

				if(result == 1) System.out.println("게시판 글 수정이 되었습니다");
				else  throw new Exception("게시판 글 수정이 되지 않았습니다 (정보를 확인해 주세요)");

				//DB에 공지 등록 처리 - BoardUpdateservice -BoardDAO 기존 프로젝트에 썻던걸 사용
				
				//response는 dispacher 에 있어서 넘겨서 처리한다
				//수정 처리 후 이동할 페이지 정보를"redirect:" 넘겨준다 - 스프링에서 이렇게쓴다
				jsp = "redirect:view.do?no="+no+"&inc=0&page="+pageObject.getPage()+"&perPageNum="+pageObject.getPerPageNum()+"&key="+pageObject.getKey()+"&word="+pageObject.getWord();
				
				break;
				
			case "/delete.do":
				
				strPerPageNum = request.getParameter("perPageNum");

				
				noStr = request.getParameter("no");
				no = Long.parseLong(noStr);
				//DB에 공지 등록 처리 - NoticeDelteservice  기존 프로젝트에 썻던걸 사용
				Execute.service(boardDeleteService, no);

				jsp= "redirect:list.do?&perPageNum="+strPerPageNum;
				
				break;
			
		default:
			throw new Exception("잘못된 페이지를 요청하셨습니다");
		}
		


		
		return jsp;
	}

}