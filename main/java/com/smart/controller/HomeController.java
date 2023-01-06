package com.smart.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	@Autowired
	private UserRepository userRepository;
	@GetMapping("/")
	//@ResponseBody
	private String home(Model model)
	{
		model.addAttribute("title"," SmartContact page");
		return "home";
	}
	@GetMapping("/about")
	private String about(Model model)
	{
		model.addAttribute("title"," SmartContact page");
		return "about";
	}
	@GetMapping("/signup")
	private String signup(Model model)
	{
		model.addAttribute("title","Register SmartContact page");
		model.addAttribute("user",new User());
		return "signup";
	}
	//handle for registration
	@PostMapping("/register")
	public String registerUser(@Valid@ModelAttribute("user") User user,BindingResult result1,
			@RequestParam(value="agreement",defaultValue ="false" ) boolean agreement,Model model,
			HttpSession session)
	
	{
		try {

			if(!agreement)
			{
				System.out.println("Please Accept terms and conditions");
				throw new Exception("Please Accept terms and conditions");
			}
			if(result1.hasErrors())
			{
				System.out.println("Error "+result1.toString());
				model.addAttribute("user",user);
				return "signup";
				
			}
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("ramesh.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			
			System.out.println("Agreement " +agreement);
			System.out.println("USER "+user);
			User result = this.userRepository.save(user);
		     model.addAttribute("user",new User());
		     session.setAttribute("message",  new Message("Successfully Ragistered", "alert-success") );
				return"signup";	
			
		} 
		catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user",user);
			session.setAttribute("message",  new Message("Something went wrong!!"+e.getMessage(), "alert-danger") );
			return"signup";	
		}
	}
    //handle custom login
	@GetMapping("/signin")
	public String customLogin( Model model)
	{
		model.addAttribute("title","Login Page");
		return "login";
	}
}
