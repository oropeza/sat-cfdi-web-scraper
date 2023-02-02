package com.norisoft.cfdi_scraper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SolicitudesSAT {

	public String obtenerViewStateInicialGet(String url, OkHttpClient client, String cookies) {

		try {
			Request solicitud_inicial = new Request.Builder().url(url).method("GET", null).addHeader("Cookie", cookies)
					.build();

			Response response = client.newCall(solicitud_inicial).execute();
			String contenido5 = response.body().string();

			if (contenido5.contains("Su sesi&oacute;n se esta cerrando...")) {
				System.out.println("Sesión perdida obtenerViewStateInicialGet");
				return null;
			}

			Document doc = Jsoup.parse(contenido5);

			return doc.select("#__VIEWSTATE").first().attr("value");

		} catch (IOException e) {
			e.printStackTrace();

		}
		return null;

	}

	public HashMap<String, String> obtenerViewStateAJAXPost(String url, OkHttpClient client, String view_state) {
		try {

			RequestBody inicial = new FormBody.Builder()
					.add("ctl00$ScriptManager1", "ctl00$MainContent$UpnlBusqueda|ctl00$MainContent$BtnBusqueda")
					.add("ctl00$MainContent$TxtUUID", "").add("ctl00$MainContent$FiltroCentral", "RdoFechas")
					.add("ctl00$MainContent$CldFecha$DdlAnio", "2020").add("ctl00$MainContent$CldFecha$DdlMes", "1")
					.add("ctl00$MainContent$CldFecha$DdlDia", "0").add("ctl00$MainContent$CldFecha$DdlHora", "0")
					.add("ctl00$MainContent$CldFecha$DdlMinuto", "0").add("ctl00$MainContent$CldFecha$DdlSegundo", "0")
					.add("ctl00$MainContent$CldFecha$DdlHoraFin", "23")
					.add("ctl00$MainContent$CldFecha$DdlMinutoFin", "59")
					.add("ctl00$MainContent$CldFecha$DdlSegundoFin", "59").add("ctl00$MainContent$TxtRfcReceptor", "")
					.add("ctl00$MainContent$DdlEstadoComprobante", "-1").add("ctl00$MainContent$hfInicialBool", "false")
					.add("ctl00$MainContent$hfDescarga", "").add("ctl00$MainContent$ddlComplementos", "-1")
					.add("ctl00$MainContent$ddlVigente", "0").add("ctl00$MainContent$ddlCancelado", "0")
					.add("ctl00$MainContent$hfParametrosMetadata", "").add("__EVENTTARGET", "")
					.add("__EVENTARGUMENT", "").add("__LASTFOCUS", "").add("__VIEWSTATE", view_state)
					.add("__VIEWSTATEENCRYPTED", "").add("__ASYNCPOST", "true")
					.add("ctl00$MainContent$BtnBusqueda", "Buscar CFDI").build();

			Request request = new Request.Builder().url(url).method("POST", inicial)
					.addHeader("X-MicrosoftAjax", "Delta=true").addHeader("X-Requested-With", "XMLHttpRequest")
					.addHeader("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36")
					.build();

			Response response5 = client.newCall(request).execute();

			String contenido5 = response5.body().string();

			String procesado[] = contenido5.split("\\|");

			if (contenido5.contains("Su sesi&oacute;n se esta cerrando...")) {
				System.out.println("Sesión perdida obtenerViewStateAJAXPost");
				return null;
			}

			int index_viewstate = Arrays.asList(procesado).indexOf("__VIEWSTATE") + 1;
			int index_viewstate_generator = Arrays.asList(procesado).indexOf("__VIEWSTATEGENERATOR") + 1;

			HashMap<String, String> resultado = new HashMap<String, String>();

			resultado.put("__VIEWSTATE", procesado[index_viewstate]);
			resultado.put("__VIEWSTATEGENERATOR", procesado[index_viewstate_generator]);

			return resultado;

		} catch (IOException e) {
			e.printStackTrace();

		}
		return null;
	}

}
