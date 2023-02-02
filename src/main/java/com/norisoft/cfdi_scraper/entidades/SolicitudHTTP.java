package com.norisoft.cfdi_scraper.entidades;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudHTTP {

	String cookies;
	byte[] captcha;
	ArrayList<Resultado> resultados;

}
