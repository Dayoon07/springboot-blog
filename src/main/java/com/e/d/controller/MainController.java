package com.e.d.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.e.d.model.entity.BoardEntity;
import com.e.d.model.entity.CommentEntity;
import com.e.d.model.entity.MemberEntity;
import com.e.d.model.repository.BoardRepository;
import com.e.d.model.repository.CommentRepository;
import com.e.d.model.repository.MemberRepository;
import com.e.d.model.service.BoardService;
import com.e.d.model.service.CommentService;
import com.e.d.model.service.MemberService;
import com.e.d.model.service.PagingService;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MainController {
	
	@Autowired
	private BoardService boardService;
	
	@Autowired
	private CommentService commentService;
	
	@Autowired
	private MemberService memberService;
	
	@Autowired
	private BoardRepository boardRepository;
	
	@Autowired
	private CommentRepository commentRepository;
	
	@Autowired
	private MemberRepository memberRepository;
	
	@Autowired
	private PagingService pagingService;
	
	@GetMapping("/")
	public String index(Model model,
	                    @RequestParam(defaultValue = "0") int page,
	                    @RequestParam(defaultValue = "20") int size) {
	    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "blogid"));
	    Page<BoardEntity> blog = boardService.getBoardWithCommentCount(pageable);
	    
	    // 페이지네이션 정보 생성
	    String pageBar = pagingService.generatePageBar(blog, "/");

	    // 페이지네이션 메시지
	    String paginationInfo = "총 " + blog.getTotalElements() + "개의 글 중 " 
	                            + (page * size + 1) + "-" 
	                            + Math.min((page + 1) * size, blog.getTotalElements()) + "번째 글";
	    
	    model.addAttribute("pageBar", pageBar);
	    model.addAttribute("paginationInfo", paginationInfo);
	    model.addAttribute("findAllBlogs", blog.getContent());
	    return "index";
	}
	
	@GetMapping("signin")
	String signin() {
		return "user/signin";
	}

	@GetMapping("signup")
	String signup() {
		return "user/signup";
	}
	
	@PostMapping("/login")
	String signinForm(
			@RequestParam String username,
			@RequestParam String userpassword,
			HttpSession session) {
		Optional<MemberEntity> user = memberRepository.findByUsernameAndUserpassword(username, userpassword);
		
		if (!username.isEmpty() && user.get().getUsername().equals(username)) {
			if (!userpassword.isEmpty() && user.get().getUserpassword().equals(userpassword)) {
				session.setAttribute("userSession", user.get());
			}
		}
		
		return "redirect:/";
	}
	
	@PostMapping("/signupForm")
	String signupForm(@ModelAttribute MemberEntity entity) {
		try {
			if (entity.getJoindate() != null) {
				memberRepository.save(entity);
			} else {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
				entity.setJoindate(LocalDateTime.now().format(formatter));
				memberRepository.save(entity);
			}
			return "index";
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/";
		}
	}
	
	@PostMapping("/logout")
	String logout(@RequestParam long memberid, HttpSession session) {
	    MemberEntity user = (MemberEntity) session.getAttribute("userSession");
	    
	    if (user != null && user.getMemberid() == memberid) {
	    	try {
	    		session.invalidate(); // 세션 무효화
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }
	    return "redirect:/";
	}
	
	@GetMapping("/profile/{username}")
	public String userProfile(@PathVariable String username, Model model) {
	    if (username == null || username.isEmpty()) {
	        model.addAttribute("UserNotFoundErrorMessage", "존재하지 않는 유저이거나 삭제된 유저입니다.");
	        return "e/UserNotFound";
	    }

	    Optional<MemberEntity> member = memberRepository.findByUsername(username);
	    if (member.isPresent()) {
	        MemberEntity user = member.get();
	        model.addAttribute("userProfileList", user);
	        model.addAttribute("userProfileName", user.getUsername().trim().toUpperCase().substring(0, 1));
	        return "user/profile";
	    } else {
	        model.addAttribute("UserNotFoundErrorMessage", "존재하지 않는 유저이거나 삭제된 유저입니다.");
	        return "e/UserNotFound";
	    }
	}
	
	@GetMapping("/board/write")
	String writeBoard() {
		return "board/write";
	}
	
	@PostMapping("/createPost")
	String createBoard(@ModelAttribute BoardEntity board, HttpSession session) {
		try {
			if (board.getDatetime() == null) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");
				board.setDatetime(LocalDateTime.now().format(formatter));
				boardRepository.save(board);
			} else {
				boardRepository.save(board);
			}
			return "redirect:/";
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/";
		}
	}
	
	@GetMapping("/blog/board")
	String particularBoard(@RequestParam long blogid,
			@RequestParam String writer,
			@RequestParam String title,
			HttpSession session,
			Model model) {
		Optional<BoardEntity> optionalBlog = boardRepository.findById(blogid);
		MemberEntity member = (MemberEntity) session.getAttribute("userSession");
		BoardEntity blog = optionalBlog.get();
		List<CommentEntity> comments = commentRepository.findByCommentasblogidOrderByCommentidDesc(blogid);
		
		if (!optionalBlog.isPresent()) {
			model.addAttribute("NotFoundBlog", "존재하지 않는 글입니다.");
			return "e/NotFoundBlog";
		}
		
	    if (member != null) {
	        blog.incrementViews();
	        boardRepository.save(blog);
	    }
	    
	    model.addAttribute("allComment", comments);
	    model.addAttribute("commentSize", comments.size());
	    model.addAttribute("particularBlog", blog);
		model.addAttribute("particularBlogAndFindAllBlogs", boardRepository.findAll(Sort.by(Sort.Direction.DESC, "blogid")));
		return "board/blog";
	}
	
	@PostMapping("/blogLike")
	String blogLike(HttpSession session, Model model,
			@RequestParam long blogid,
			@RequestParam String writer,
			@RequestParam String title) {
	    MemberEntity user = (MemberEntity) session.getAttribute("userSession");
	    if (user == null) {
	        model.addAttribute("errorMessage", "로그인이 필요합니다.");
	        return "redirect:/signin";
	    }
	    
	    try {
	    	String encodedWriter = URLEncoder.encode(writer, StandardCharsets.UTF_8.toString());
	    	String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
	    	boardService.toggleLike(blogid, user.getMemberid());
		    return "redirect:/blog/board?blogid=" + blogid + "&writer=" + encodedWriter + "&title=" + encodedTitle;
		} catch (Exception e) {
			e.printStackTrace();
			return "e/error500";
		}
	}
	
	@PostMapping("/commentAdd")
	String addComment(
	        @RequestParam long blogid,
	        @RequestParam String writer,
	        @RequestParam String title,
	        @RequestParam String blogboardTitle,
	        @RequestParam String commenter,
	        @RequestParam String commentcontent, Model m) {
	    try {
	    	boardRepository.findById(blogid).orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 글입니다."));

	        CommentEntity addComment = CommentEntity.builder()
	                .blogboardTitle(blogboardTitle)
	                .commentasblogid(blogid)
	                .commenter(commenter)
	                .commentContent(commentcontent)
	                .commentDatetime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")))
	                .build();

	        commentRepository.save(addComment);
	    } catch (IllegalArgumentException e) {
	        m.addAttribute("EmptyBlog", e.getMessage());
	        return "e/EmptyBlog";
	    } catch (Exception e) {
	        m.addAttribute("errorMessage", "댓글 저장 중 문제가 발생했습니다.");
	        return "e/Error";
	    }
	    return "redirect:/blog/board?blogid=" + blogid + 
	    	       "&writer=" + URLEncoder.encode(writer, StandardCharsets.UTF_8) + 
	    	       "&title=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
