package com.norisoft.cfdi_scraper.entidades;

public enum EstadoComprobante {

	TODOS("-1"), CANCELADO("0"), VIGENTE("1");

	EstadoComprobante(String string) {
		this.estado = string;
	}

	private String estado;

	public String getEstado() {
		return estado;
	}

}
