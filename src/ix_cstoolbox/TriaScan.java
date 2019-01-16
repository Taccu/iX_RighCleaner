/*
 * To change this license header;@XmlElement() choose License Headers in Project Properties.
 * To change this template file;@XmlElement() choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * ;@author bho
 */
@XmlRootElement(name="root")
@XmlAccessorType(XmlAccessType.FIELD)
public class TriaScan {
        @XmlElement(name="PrescriberName")
        public String PrescriberName;
        @XmlElement(name="PrescriberContractNumber")
        public String PrescriberContractNumber;
        @XmlElement(name="PatientZipCode")
        public String PatientZipCode;
        @XmlElement(name="PatientNumber")
        public String PatientNumber;
        @XmlElement(name="PatientLastName")
        public String PatientLastName;
        @XmlElement(name="PatientFirstName")
        public String PatientFirstName;
        @XmlElement(name="PatientDateOfBirth")
        public String PatientDateOfBirth;
        @XmlElement(name="PatientCity")
        public String PatientCity;
        @XmlElement(name="PatientAddress")
        public String PatientAddress;
        @XmlElement(name="OUZipCode")
        public String OUZipCode;
        @XmlElement(name="OUName") 
        public String OUName;
              @XmlElement(name="OUCode") 
        public String OUCode;
              @XmlElement(name="OUCity;") 
        public String OUCity;
              @XmlElement(name="OUAddress") 
        public String OUAddress;
              @XmlElement(name="ImageGUID") 
        public String ImageGUID;
              @XmlElement(name="DocumentType") 
        public String DocumentType;
              @XmlElement(name="DocumentNumber") 
        public String DocumentNumber;
              @XmlElement(name="DocumentEAN1") 
        public String DocumentEAN1;
              @XmlElement(name="DocumentEAN0") 
        public String DocumentEAN0;
              @XmlElement(name="DocumentDate") 
        public String DocumentDate;
              @XmlElement(name="CompanyCode") 
        public String CompanyCode;
              @XmlElement(name="BatchNumber") 
        public String BatchNumber;
              @XmlElement(name="BatchEAN1") 
        public String BatchEAN1;
              @XmlElement(name="BatchEAN0") 
        public String BatchEAN0;
              @XmlElement(name="BatchDate") 
        public String BatchDate;
              @XmlElement(name="InsuranceNumber") 
        public String InsuranceNumber;
        @XmlElement(name="InsuranceName") 
        public String InsuranceName;
        public Long id;
        
        public TriaScan() {
            
        }

    public String getPrescriberName() {
        return PrescriberName;
    }

    public void setPrescriberName(String PrescriberName) {
        this.PrescriberName = PrescriberName;
    }

    public String getPrescriberContractNumber() {
        return PrescriberContractNumber;
    }

    public void setPrescriberContractNumber(String PrescriberContractNumber) {
        this.PrescriberContractNumber = PrescriberContractNumber;
    }

    public String getPatientZipCode() {
        return PatientZipCode;
    }

    public void setPatientZipCode(String PatientZipCode) {
        this.PatientZipCode = PatientZipCode;
    }

    public String getPatientNumber() {
        return PatientNumber;
    }

    public void setPatientNumber(String PatientNumber) {
        this.PatientNumber = PatientNumber;
    }

    public String getPatientLastName() {
        return PatientLastName;
    }

    public void setPatientLastName(String PatientLastName) {
        this.PatientLastName = PatientLastName;
    }

    public String getPatientFirstName() {
        return PatientFirstName;
    }

    public void setPatientFirstName(String PatientFirstName) {
        this.PatientFirstName = PatientFirstName;
    }

    public String getPatientDateOfBirth() {
        return PatientDateOfBirth;
    }

    public void setPatientDateOfBirth(String PatientDateOfBirth) {
        this.PatientDateOfBirth = PatientDateOfBirth;
    }

    public String getPatientCity() {
        return PatientCity;
    }

    public void setPatientCity(String PatientCity) {
        this.PatientCity = PatientCity;
    }

    public String getPatientAddress() {
        return PatientAddress;
    }

    public void setPatientAddress(String PatientAddress) {
        this.PatientAddress = PatientAddress;
    }

    public String getOUZipCode() {
        return OUZipCode;
    }

    public void setOUZipCode(String OUZipCode) {
        this.OUZipCode = OUZipCode;
    }

    public String getOUName() {
        return OUName;
    }

    public void setOUName(String OUName) {
        this.OUName = OUName;
    }

    public String getOUCode() {
        return OUCode;
    }

    public void setOUCode(String OUCode) {
        this.OUCode = OUCode;
    }

    public String getOUCity() {
        return OUCity;
    }

    public void setOUCity(String OUCity) {
        this.OUCity = OUCity;
    }

    public String getOUAddress() {
        return OUAddress;
    }

    public void setOUAddress(String OUAddress) {
        this.OUAddress = OUAddress;
    }

    public String getImageGUID() {
        return ImageGUID;
    }

    public void setImageGUID(String ImageGUID) {
        this.ImageGUID = ImageGUID;
    }

    public String getDocumentType() {
        return DocumentType;
    }

    public void setDocumentType(String DocumentType) {
        this.DocumentType = DocumentType;
    }

    public String getDocumentNumber() {
        return DocumentNumber;
    }

    public void setDocumentNumber(String DocumentNumber) {
        this.DocumentNumber = DocumentNumber;
    }

    public String getDocumentEAN1() {
        return DocumentEAN1;
    }

    public void setDocumentEAN1(String DocumentEAN1) {
        this.DocumentEAN1 = DocumentEAN1;
    }

    public String getDocumentEAN0() {
        return DocumentEAN0;
    }

    public void setDocumentEAN0(String DocumentEAN0) {
        this.DocumentEAN0 = DocumentEAN0;
    }

    public String getDocumentDate() {
        return DocumentDate;
    }

    public void setDocumentDate(String DocumentDate) {
        this.DocumentDate = DocumentDate;
    }

    public String getCompanyCode() {
        return CompanyCode;
    }

    public void setCompanyCode(String CompanyCode) {
        this.CompanyCode = CompanyCode;
    }

    public String getBatchNumber() {
        return BatchNumber;
    }

    public void setBatchNumber(String BatchNumber) {
        this.BatchNumber = BatchNumber;
    }

    public String getBatchEAN1() {
        return BatchEAN1;
    }

    public void setBatchEAN1(String BatchEAN1) {
        this.BatchEAN1 = BatchEAN1;
    }

    public String getBatchEAN0() {
        return BatchEAN0;
    }

    public void setBatchEAN0(String BatchEAN0) {
        this.BatchEAN0 = BatchEAN0;
    }

    public String getBatchDate() {
        return BatchDate;
    }

    public void setBatchDate(String BatchDate) {
        this.BatchDate = BatchDate;
    }

    public String getInsuranceNumber() {
        return InsuranceNumber;
    }

    public void setInsuranceNumber(String InsuranceNumber) {
        this.InsuranceNumber = InsuranceNumber;
    }

    public String getInsuranceName() {
        return InsuranceName;
    }

    public void setInsuranceName(String InsuranceName) {
        this.InsuranceName = InsuranceName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
        
        
    }