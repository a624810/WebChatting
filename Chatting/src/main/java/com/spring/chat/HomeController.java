package com.spring.chat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.spring.dto.MemberVO;
import com.spring.dto.NoticeVO;
import com.spring.dto.RoomDTO;
import com.spring.mapper.NoticeMapper;
import com.spring.mapper.RoomMapper;
import com.spring.service.RoomService;
import com.spring.chat.HomeController;
import com.spring.chat.MediaUtils;
import com.spring.chat.UploadFileUtils;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
 
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
   
    @Autowired
    private SqlSession sqlSession;
    
    @Resource(name="uploadPath")
	String uploadPath;
  
    
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(Locale locale, Model model) {
    	NoticeMapper noticeMapper = sqlSession.getMapper(NoticeMapper.class);
    	List<NoticeVO> noticeVoList = noticeMapper.SelectNotices();
    	
    	model.addAttribute("notice", noticeVoList);
    	
    	
    	
    	RoomMapper mapper = sqlSession.getMapper(RoomMapper.class);
    	int roomCount = mapper.countColumns();
    	
    	RoomDTO room[] = new RoomDTO[roomCount];
    	for (int i = 0; i < roomCount; i++) {
	        RoomDTO mapperRoom = mapper.selectRooms().get(i);
	        room[i] = new RoomDTO();
	        room[i].setId(mapperRoom.getId());
	        room[i].setSubject(mapperRoom.getSubject());
	        room[i].setMaster(mapperRoom.getMaster());
    	}
	        model.addAttribute("room", room);
    	
        return "home";
 }
    @RequestMapping(value = "/chat", method=RequestMethod.GET)
    public String chat(Model model, @RequestParam(value="roomID")String roomID, HttpServletRequest req) {
    	HttpSession session = req.getSession();
    	
    	MemberVO login = new MemberVO();
    	login = (MemberVO)session.getAttribute("loginid");
    	String loginid = login.getId();
    	session.setAttribute("roomID", roomID);

    	model.addAttribute("id", loginid);
    	
    	RoomMapper room = sqlSession.getMapper(RoomMapper.class); 	
    	RoomDTO mapperRoom = room.selectRoomInfo(roomID);
    	model.addAttribute("subject", mapperRoom.getSubject());
    	model.addAttribute("master", mapperRoom.getMaster());
    	
    	System.out.println(loginid + "   1  " + mapperRoom.getMaster());
    	return "chat";
    }
    
    @Inject
    RoomService Rservice;
    
    @RequestMapping(value = "/make", method=RequestMethod.GET)
    public void getmake() throws Exception {}
    
    @RequestMapping(value = "/makenotice", method=RequestMethod.GET)
    public void GetMakeNotice() throws Exception {}
    
    @RequestMapping(value = "/makenotice", method=RequestMethod.POST)
    public String PostMakeNotice(String comment, HttpServletResponse response) throws Exception {
    	NoticeMapper noticeMapper = sqlSession.getMapper(NoticeMapper.class);
    	noticeMapper.InsertNotice(comment);
    	
    	response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();	 
		out.println("<script>opener.parent.location.reload(); window.close();</script>");
		out.flush();
		
    	return "redirect:/";
    }
    
    @RequestMapping(value="/delete", method=RequestMethod.POST)
    public String PostDelete(String roomId, HttpServletResponse response, HttpServletRequest req) throws Exception{
    	
    	RoomMapper mapper = sqlSession.getMapper(RoomMapper.class);
    	mapper.DeleteRoom(Integer.parseInt(roomId));
       
    	
    	response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();	 
		out.println("<script>alert('방이 삭제되었습니다.'); opener.parent.location.reload(); window.close();</script>");
		out.flush();
		
		 
    	return "";
    }
    
    @RequestMapping(value="/make", method=RequestMethod.POST)
    public String postmake(@Valid RoomDTO dto, HttpServletResponse response,RedirectAttributes rttr, HttpServletRequest req, BindingResult bindingResult) throws Exception{
    
    	
    	
    	HttpSession session = req.getSession();
    	
    
    	System.out.println(dto.getMaster());
    	
    	RoomMapper mapper = sqlSession.getMapper(RoomMapper.class);
    	List<RoomDTO> roomDtoList = new ArrayList<RoomDTO>();
    	roomDtoList = mapper.selectRooms();
        
    	int roomcount = 1;
    	for(RoomDTO roomDto : roomDtoList) {
    		if(Integer.parseInt(roomDto.getId()) != roomcount) {
    			break;
    		}
    		roomcount +=1;
    	}
    	String roomId = Integer.toString(roomcount);
	
		dto.setId(roomId);
    	dto.setMaster(((MemberVO)session.getAttribute("loginid")).getId());
    	mapper.InsertRoom(dto);
    	response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();	 
		out.println("<script>opener.parent.location.reload(); window.close();</script>");
		out.flush();
		return null;
		
    	
    }
    	
    
    
    
    // 5. Ajax업로드 처리 매핑
    // 파일의 한글처리 : produces="text/plain;charset=utf-8"
    @ResponseBody // view가 아닌 data리턴
    @RequestMapping(value="/upload/uploadAjax", method=RequestMethod.POST, produces="text/plain;charset=utf-8")
    public ResponseEntity<String> uploadAjax(MultipartFile file) throws Exception {
        logger.info("originalName : "+file.getOriginalFilename(), "utf-8");
        logger.info("size : "+file.getSize());
        logger.info("contentType : "+file.getContentType());
        return new ResponseEntity<String>(UploadFileUtils.uploadFile(uploadPath, file.getOriginalFilename(), file.getBytes()), HttpStatus.OK);
    }

    // 6. 이미지 표시 매핑
    @ResponseBody // view가 아닌 data리턴
    @RequestMapping("/upload/displayFile")
    public ResponseEntity<byte[]> displayFile(String fileName) throws Exception {
        // 서버의 파일을 다운로드하기 위한 스트림
        InputStream in = null; //java.io
        ResponseEntity<byte[]> entity = null;
        try {
            // 확장자를 추출하여 formatName에 저장
            String formatName = fileName.substring(fileName.lastIndexOf(".") + 1);
            // 추출한 확장자를 MediaUtils클래스에서  이미지파일여부를 검사하고 리턴받아 mType에 저장
            MediaType mType = MediaUtils.getMediaType(formatName);
            // 헤더 구성 객체(외부에서 데이터를 주고받을 때에는 header와 body를 구성해야하기 때문에)
            HttpHeaders headers = new HttpHeaders();
            // InputStream 생성
            in = new FileInputStream(uploadPath + fileName);
            // 이미지 파일이면
            if (mType != null) { 
                headers.setContentType(mType);
            // 이미지가 아니면
            } else { 
                fileName = fileName.substring(fileName.indexOf("_") + 1);
                // 다운로드용 컨텐트 타입
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                // 
                // 바이트배열을 스트링으로 : new String(fileName.getBytes("utf-8"),"iso-8859-1") * iso-8859-1 서유럽언어, 큰 따옴표 내부에  " \" 내용 \" "
                // 파일의 한글 깨짐 방지
                headers.add("Content-Disposition", "attachment; filename=\""+new String(fileName.getBytes("utf-8"), "iso-8859-1")+"\"");
                //headers.add("Content-Disposition", "attachment; filename='"+fileName+"'");
            }
            // 바이트배열, 헤더, HTTP상태코드
            entity = new ResponseEntity<byte[]>(IOUtils.toByteArray(in), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            // HTTP상태 코드()
            entity = new ResponseEntity<byte[]>(HttpStatus.BAD_REQUEST);
        } finally {
            in.close(); //스트림 닫기
        }
        return entity;
    }
}


