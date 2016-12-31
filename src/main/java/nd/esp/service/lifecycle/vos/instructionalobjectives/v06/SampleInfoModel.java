package nd.esp.service.lifecycle.vos.instructionalobjectives.v06;

import java.util.List;

import nd.esp.service.lifecycle.repository.model.Sample;

public class SampleInfoModel {

	
	private String sampleCreateTime;

	private String sampleName;

	private String sampleAuthorName;

	private int sampleInsObjTotal;
	
	private int sampleInsObjAndSubInsObjTotal;
	
	private String sampleStatu;
	
	public String getSampleStatu() {
		return sampleStatu;
	}

	public void setSampleStatu(String sampleStatu) {
		this.sampleStatu = sampleStatu;
	}

	public String getSampleCreateTime() {
		return sampleCreateTime;
	}

	public void setSampleCreateTime(String sampleCreateTime) {
		this.sampleCreateTime = sampleCreateTime;
	}

	public String getSampleName() {
		return sampleName;
	}

	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
	}

	public String getSampleAuthorName() {
		return sampleAuthorName;
	}

	public void setSampleAuthorName(String sampleAuthorName) {
		this.sampleAuthorName = sampleAuthorName;
	}

	public int getSampleInsObjTotal() {
		return sampleInsObjTotal;
	}

	public void setSampleInsObjTotal(int sampleInsObjTotal) {
		this.sampleInsObjTotal = sampleInsObjTotal;
	}

	public int getSampleInsObjAndSubInsObjTotal() {
		return sampleInsObjAndSubInsObjTotal;
	}

	public void setSampleInsObjAndSubInsObjTotal(int sampleInsObjAndSubInsObjTotal) {
		this.sampleInsObjAndSubInsObjTotal = sampleInsObjAndSubInsObjTotal;
	}

	
	
}
