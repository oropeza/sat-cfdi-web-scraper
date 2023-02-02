package com.norisoft.cfdi_scraper.entidades;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Resultado {

	private String uuid;
	private String rfcEmisor;
	private String nombreEmisor;
	private String rfcReceptor;
	private String nombreReceptor;
	private String fechaEmision;
	private String fechaCertificacion;
	private String pacCertifico;
	private String total;
	private String efectoComprobante;
	private String estatusCancelacion;
	private String estadoComprobante;
	private String estatusProcesoCancelacion;
	private String fechaProcesoCancelacion;

	public String getFechaEmision() {
		return fechaEmision.replace("T", " ");
	}

}
