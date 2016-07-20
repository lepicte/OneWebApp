package com.gab.onewebapp.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.gab.onewebapp.beans.form.FileUpload;
import com.gab.onewebapp.beans.form.FilesUploadForm;
import com.gab.onewebapp.config.ApplicationCommonConfig;
import com.gab.onewebapp.dao.FileDao;
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

/**
 * @author gabriel
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextHierarchy({
	@ContextConfiguration("file:src/main/webapp/WEB-INF/spring/root-context.xml"),
	@ContextConfiguration("file:src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml"),
	@ContextConfiguration("classpath:/spring/security-context-test.xml")
//	@ContextConfiguration("file:src/main/webapp/WEB-INF/spring/security-context.xml")
})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
	DirtiesContextTestExecutionListener.class,
	TransactionalTestExecutionListener.class,
	DbUnitTestExecutionListener.class })
@Transactional
@DatabaseSetup("/dbtest/sample-fileDaoTest.xml")
public class FileControllerTest {

	private static final Logger logger = LoggerFactory.getLogger(FileControllerTest.class);
	
	@Autowired
    private WebApplicationContext wac;
	
//	@Autowired
//	private Filter springSecurityFilterChain;
	
	@Autowired
	private FileDao fileDao;
	
	@Autowired
	private ApplicationCommonConfig appConfig;
	
	@Autowired
	private MessageSource messageSource;
	
	private MockMvc mockMvc;
	
	@Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders
        		.webAppContextSetup(this.wac)
        		.apply(springSecurity())
        		.build();       
    }
	
	@After
	public void tearDown() throws IOException{
		String filesUploadedPath = this.appConfig.getUploadDirPath();

		FileUtils.cleanDirectory(new File(filesUploadedPath));
		new File(filesUploadedPath + File.separator + ".keep").createNewFile();
	}
	
	@Test
	@Transactional
	public void should_be_success_when_file_uploaded() throws Exception {
		final String fileName = "test.txt";
		final byte[] content = "Hello Word".getBytes();
		MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "text/plain", content);
		
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		authorities.add(new SimpleGrantedAuthority("PERM_UPLOAD_FILE"));
		
		FileUpload fileUpload = new FileUpload();
		fileUpload.setFile(mockMultipartFile);
		fileUpload.setDescription("fichier test");
		
		List<FileUpload> listFileUpload = new ArrayList<FileUpload>();
		listFileUpload.add(fileUpload);
		
		FilesUploadForm filesUploadForm = new FilesUploadForm();
		filesUploadForm.setFilesUploaded(listFileUpload);	
		
		//TODO: check why csrf() is not working
		this.mockMvc.perform(
				post(FileController.ROUTE_UPLOAD_FILE)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)					
				.flashAttr("filesUploadForm",filesUploadForm)	
				.with(user("user1").authorities(authorities))
				.with(csrf())
		)
		.andExpect(status().isOk())
		.andExpect(view().name(FileController.VIEW_SHOW_FILES));
		
		assertFalse(fileDao.findByOriginalFilename(null,fileName).isEmpty());
		assertEquals(fileDao.findByOriginalFilename(null,fileName).get(0).getDescription(),"fichier test");			
			
	}
	
	@Test
	@Transactional
	public void should_be_succes_when_showing_user_files() throws Exception {

		this.mockMvc.perform(
				get(FileController.ROUTE_SHOW_FILES)			
				.with(user("user1").roles("USER"))
				.with(csrf())
		)
		.andExpect(status().isOk())
		.andExpect(model().attribute("listFiles", hasSize(1)))
		.andExpect(view().name(FileController.VIEW_SHOW_FILES));
	}
	
	@Test
	@Transactional
	public void should_be_succes_when_deleting_user_files() throws Exception {

		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		authorities.add(new SimpleGrantedAuthority("PERM_DELETE_FILE"));
		
		this.mockMvc.perform(
				get(FileController.ROUTE_DELETE_FILE)	
				.param("id", "0")
				.with(user("user1").authorities(authorities))
				.with(csrf())
		)
		.andExpect(status().isOk())
		.andExpect(model().attribute("listFiles", hasSize(0)))
		.andExpect(view().name(FileController.VIEW_SHOW_FILES));
	}
	
	@Test
	@Transactional
	public void should_be_success_when_downloading_user_files() throws Exception {

		FileUtils.writeStringToFile(new File(this.appConfig.getUploadDirPath() + File.separator + "0" + File.separator + "fichier1.txt"),"fichier de test 1");
		
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		authorities.add(new SimpleGrantedAuthority("PERM_DOWNLOAD_FILE"));
		
		this.mockMvc.perform(
				get(FileController.ROUTE_DOWNLOAD_FILE)	
				.param("id", "0")
				.with(user("user1").authorities(authorities))
				.with(csrf())
		)
		.andExpect(status().isOk())
		.andExpect(content().string("fichier de test 1"));
	}
	
	@Test
	@Transactional
	public void should_be_failure_when_downloading_user_files() throws Exception {
		
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		authorities.add(new SimpleGrantedAuthority("PERM_DOWNLOAD_FILE"));
		
		LocaleContextHolder.setLocale(new Locale("fr", "FR"));
		String errorMessage = this.messageSource.getMessage("fileController.label.download_error", null, LocaleContextHolder.getLocale());
		byte[] arrayByteResExpected = Charset.forName("UTF-8").encode(errorMessage).array();
		
		this.mockMvc.perform(
				get(FileController.ROUTE_DOWNLOAD_FILE)	
				.param("id", "0")
				.with(user("user1").authorities(authorities))
				.with(csrf())
		)
		.andExpect(status().isOk())
		.andExpect(content().bytes(Arrays.copyOfRange(arrayByteResExpected, 0, arrayByteResExpected.length - 1)));
	}
	
}
