package com.gab.onewebapp.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * @author gabriel
 * 
 */
@Entity
@Table(name = "TFILE")
public class FileEntity {

	@Id
	@GeneratedValue
	private long id;
	
	@Column(name="original_filename", nullable = false, length = 255)
	private String originalFilename;
	
	@Column(name="stored_filename", nullable = false, length = 255)
	private String storedFilename;
	
	@Column(name="description", length = 255)
	private String description;

	@Column(name="date_upload", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date dateUpload = new Date();

	@Column(nullable = false)
	private long version = 1;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="user_id", nullable = false)
	private UserEntity user;

	public FileEntity(){}
	
	public FileEntity(UserEntity user, String originalFilename, String storedFilename, String description){
		this.user = user;
		this.originalFilename = originalFilename;
		this.storedFilename = storedFilename;
		this.description = description;
	}
	
	public FileEntity(UserEntity user, String originalFilename, String storedFilename, String description, Date dateUpload){
		this(user, originalFilename, storedFilename, description);
		this.dateUpload = dateUpload;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public String getStoredFilename() {
		return storedFilename;
	}

	public void setStoredFilename(String storedFilename) {
		this.storedFilename = storedFilename;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDateUpload() {
		return dateUpload;
	}

	public void setDateUpload(Date dateUpload) {
		this.dateUpload = dateUpload;
	}
		
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);	
	}

	@Override
	public int hashCode() {
		return (int) (this.id * this.originalFilename.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		/*
		 * Une solution serait d'utiliser la réflection avec la lib apache mais + lent
		 * et il n'est pas nécessaire de tester tous les champs, à minima ceux utilisé
		 * dans le hashCode
		 * 
		 * return EqualsBuilder.reflectionEquals(this, obj);
		 */
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		FileEntity other = (FileEntity) obj;
		return new EqualsBuilder()
			.append(this.getId(), other.getId())
			.append(this.getOriginalFilename(), other.getOriginalFilename())
			.isEquals();
	}
}

