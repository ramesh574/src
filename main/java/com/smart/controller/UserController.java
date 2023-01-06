package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal)
	{
		String userName = principal.getName();
		System.out.println("username "+userName);
		//get the user using username(email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("User "+user);
		model.addAttribute("user",user);
	}
	
	//dashbord home
	@RequestMapping("/index")
	public String index( Model model,Principal principal)
	{
		/*
		 * String userName = principal.getName();
		 * System.out.println("username "+userName); //get the user using
		 * username(email) User user = userRepository.getUserByUserName(userName);
		 * System.out.println("User "+user); model.addAttribute("user",user);
		 */
		model.addAttribute("title","Home");
		return"normal/user-dashboard";
	} 
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return"normal/addcontact";
	}
	@PostMapping("/process-contact")
	public String proccessContact(@ModelAttribute Contact contact
			,@RequestParam("profileImage")MultipartFile file,Principal principal,Model model) {
		try {
		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);
		//processing and uploading file
		if(file.isEmpty())
		{
			System.out.println("no image is uploading");
			contact.setImage("contact.png");
		}
		else
		{
			contact.setImage(file.getOriginalFilename());
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
			System.out.println("image is uploaded");
		}
		
		
		contact.setUser(user);
		
		user.getContacts().add(contact);
	      userRepository.save(user);
		System.out.println("data "+contact);
		System.out.println("Add data in database");
		// massage success
		//session.setAttribute("message",new Message("Your Contact is added !! Add more contacts..","success"));
		model.addAttribute("message", "Successfully added data ");
		
		}
		catch(Exception e)
		{
			System.out.println("Error"+e.getMessage());
			e.printStackTrace();
			//message error
			//session.setAttribute("message",new Message("Something went wrong Please !!Try Again","danger"));
		}
		return "normal/addcontact";
		
	}
	@GetMapping("/show-contact/{page}")
	//per page=10[n]
	//current page=0[page]
	public String showContact(@PathVariable("page") Integer page,  Model m,Principal principal)
	{
		m.addAttribute("title","Show Contact");
		//we want send details of contact
		/*
		 * String userName = principal.getName(); User user =
		 * userRepository.getUserByUserName(userName); List<Contact> contacts =
		 * user.getContacts();
		 */
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		PageRequest pageable = PageRequest.of(page, 8);
		Page<Contact> contacts = contactRepository.findContactByUser(user.getId(),pageable);
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPage",contacts.getTotalPages());
		
		return"normal/showcontact";
	}
	//Showing particular contact details
	@GetMapping("/{cId}/contact")
	public String showContactDeatail(@PathVariable("cId") Integer cId,Model model ,Principal principal)
	{
		System.out.println("CID "+cId);
		Optional<Contact> contact = contactRepository.findById(cId);
		Contact contact2 = contact.get();
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact2.getUser().getId())
		{
			model.addAttribute("contact2", contact2);
			model.addAttribute("title", contact2.getName());
		}
		
		return "normal/contact_detail";
	}
	//delete handler form
	@GetMapping("delete/{cid}")
	public String deleteContact(@PathVariable ("cid") Integer cId,Model model,HttpSession session,Principal principal)
	{
		Contact contact = this.contactRepository.findById(cId).get();
		      //Contact contact2 = contact.get();
				/*
				 * contact.setUser(null); this.contactRepository.deleteById(cId);
				 */
		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		     // model.addAttribute("message","Successfully deleted contact");
		      session.setAttribute("message",new Message("Your Contact is added !! Add more contacts..","success"));
		      return"redirect:/user/show-contact/0";
	}
	
	//Open  update handler form
	@PostMapping("/update/{cid}")
	public String updateContact(@PathVariable("cid") Integer cid, Model m)
	{
		m.addAttribute("title","Update");
		Contact contact = this.contactRepository.findById(cid).get();
		m.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	//Update contact handler
	@RequestMapping(value="/process-update",method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage")MultipartFile file,Model m,Principal principal)
	{
		try {
			//old contact details
			Contact oldcontact = this.contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty())
			{
				//delete old photo
				File deleteFile=new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldcontact.getImage());
				file1.delete();
				//update new photo
				
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
			}
			else
			{
				contact.setImage(oldcontact.getImage());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
		}
		catch(Exception e)
		{
			
			e.printStackTrace();
		}
		
		System.out.println("CONTACT NAME "+contact.getName());
		System.out.println("CONTACT ID "+contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	//Your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model)
	{
		model.addAttribute("title","Profile Page");
		return"normal/profile";
	}
	
	//open handler settings
	@GetMapping("/setting")
	public String openSetting(Model model)
	{
		model.addAttribute("title","Setting");
		return"normal/setting";
	}
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword")String oldPassword,@RequestParam("newPassword")String newPassword,Principal principal,HttpSession session,Model model)
	{
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		if(this.bCryptPasswordEncoder.matches(oldPassword,currentUser.getPassword()))
		{
			//change the password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			 session.setAttribute("message",new Message("Your Password is changed successully","success"));
		}
		else
		{
			 //session.setAttribute("message",new Message("Your have enter wrong password","danger"));
			model.addAttribute("message", "Successfully added data ");
			 return"redirect:/user/setting";
		}
		System.out.println("OLD PASsword"+oldPassword);
		System.out.println("NEW PASS"+newPassword);
		return"redirect:/user/index";
	}
	
	

}
