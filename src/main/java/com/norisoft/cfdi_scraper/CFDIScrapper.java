package com.norisoft.cfdi_scraper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.norisoft.cfdi_scraper.entidades.EstadoComprobante;
import com.norisoft.cfdi_scraper.entidades.Resultado;
import com.norisoft.cfdi_scraper.entidades.SolicitudHTTP;

import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CFDIScrapper {

	final String SAT_LOGIN = "https://cfdiau.sat.gob.mx/nidp/app/login?id=SATUPCFDiCon&sid=0&option=credential&sid=0";
	final String SAT_PORTAL_FACTURACION = "https://portalcfdi.facturaelectronica.sat.gob.mx";
	final String SAT_CONSULTA_RECEPTOR = "https://portalcfdi.facturaelectronica.sat.gob.mx/ConsultaReceptor.aspx";
	final String SAT_CONSULTA_EMISOR = "https://portalcfdi.facturaelectronica.sat.gob.mx/ConsultaEmisor.aspx";

	public SolicitudHTTP obtenerCaptcha() {
		System.out.println("obtenerCaptcha");
		try {

			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

			OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
					.readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();

			Request request = new Request.Builder().url(SAT_LOGIN).method("GET", null).build();

			Response response = client.newCall(request).execute();

			String cookies = String.join(";", response.headers().values("Set-Cookie"));
			Document doc = Jsoup.parse(response.body().string());

			String imagen = doc.select("form#IDPLogin img").first().attr("src");
			imagen = imagen.substring(imagen.lastIndexOf(",") + 1);

			byte[] image = Base64.getDecoder().decode(imagen);

			return SolicitudHTTP.builder().captcha(image).cookies(cookies).build();
		} catch (java.net.SocketTimeoutException e) {
			System.out.println("SocketTimeoutException in obtenerCaptcha");
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public SolicitudHTTP crearSesion(String cookies, String captcha, String rfc, String clave) {

		try {
			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

			RequestBody requestBody = new FormBody.Builder()

					.add("Ecom_User_ID", rfc).add("Ecom_Password", clave).add("option", "credential")
					.add("submit", "Enviar").add("userCaptcha", captcha).build();

			OkHttpClient client = new OkHttpClient().newBuilder().cookieJar(new JavaNetCookieJar(cookieManager))
					.connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
					.writeTimeout(30, TimeUnit.SECONDS).build();

			Request request = new Request.Builder().url(SAT_LOGIN).method("POST", requestBody)
					.addHeader("Content-Type", "application/x-www-form-urlencoded").addHeader("Cookie", cookies)
					.build();

			Response response = client.newCall(request).execute();

			if (response.isSuccessful()) {
				String resultado = response.body().string();
				if (resultado.contains("https://cfdiau.sat.gob.mx/nidp/app?sid=0'")) {
					System.out.println("Ok! USER IS LOGGED");

					RequestBody empty = RequestBody.create("", MediaType.parse("text/plain"));

					Request request2 = new Request.Builder().url(SAT_PORTAL_FACTURACION).method("POST", empty).build();

					Response response2 = client.newCall(request2).execute();

					String response2_txt = response2.body().string();
					Document doc = Jsoup.parse(response2_txt);

					System.out.println(response2_txt);

					RequestBody requestBody4 = new FormBody.Builder()
							.add("wa", doc.select("input[name=wa]").first().attr("value"))
							.add("wresult", doc.select("input[name=wresult]").first().attr("value"))
							.add("wctx", doc.select("input[name=wctx]").first().attr("value")).build();

					Request request_PASO4 = new Request.Builder().url(SAT_PORTAL_FACTURACION)
							.method("POST", requestBody4).build();

					Response response4 = client.newCall(request_PASO4).execute();

					String contenido4 = response4.body().string();

					if (contenido4.contains("Solicitudes de Cancelación")) {

						System.out.println("Autenticación correcta");
						String cookies_ok = String.join(";", response4.headers().values("Set-Cookie"));
						return SolicitudHTTP.builder().cookies(cookies_ok).build();
					}

				} else {

					if (resultado.contains("Captcha no válido")) {
						System.out.println("Captcha no válido");

					} else {
						System.out.println(resultado);
					}

				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	public SolicitudHTTP buscarRecibidas(String cookies, String year, String mes, String rfc_emisor,
			EstadoComprobante estadoComprobante) {

		try {

			if (rfc_emisor == null)
				rfc_emisor = "";

			SolicitudesSAT solicitudes = new SolicitudesSAT();

			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

			OkHttpClient client = new OkHttpClient().newBuilder().cookieJar(new JavaNetCookieJar(cookieManager))
					.connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
					.writeTimeout(30, TimeUnit.SECONDS).build();

			String view_state = solicitudes.obtenerViewStateInicialGet(SAT_CONSULTA_RECEPTOR, client, cookies);
			if (view_state == null)
				return null;

			HashMap<String, String> metadatos = solicitudes.obtenerViewStateAJAXPost(SAT_CONSULTA_RECEPTOR, client,
					view_state);
			if (metadatos == null)
				return null;

			RequestBody requestBody6 = new FormBody.Builder()

					.add("ctl00$ScriptManager1", "ctl00$MainContent$UpnlBusqueda|ctl00$MainContent$BtnBusqueda")
					.add("ctl00$MainContent$TxtUUID", "").add("ctl00$MainContent$FiltroCentral", "RdoFechas")
					.add("ctl00$MainContent$CldFecha$DdlAnio", year).add("ctl00$MainContent$CldFecha$DdlMes", mes)
					.add("ctl00$MainContent$CldFecha$DdlDia", "0").add("ctl00$MainContent$CldFecha$DdlHora", "0")
					.add("ctl00$MainContent$CldFecha$DdlMinuto", "0").add("ctl00$MainContent$CldFecha$DdlSegundo", "0")
					.add("ctl00$MainContent$CldFecha$DdlHoraFin", "23")
					.add("ctl00$MainContent$CldFecha$DdlMinutoFin", "59")
					.add("ctl00$MainContent$CldFecha$DdlSegundoFin", "59")
					.add("ctl00$MainContent$TxtRfcReceptor", rfc_emisor)
					.add("ctl00$MainContent$DdlEstadoComprobante", estadoComprobante.getEstado())
					.add("ctl00$MainContent$hfInicialBool", "false").add("ctl00$MainContent$hfDescarga", "")
					.add("ctl00$MainContent$ddlComplementos", "-1").add("ctl00$MainContent$ddlVigente", "0")
					.add("ctl00$MainContent$ddlCancelado", "0").add("ctl00$MainContent$hfParametrosMetadata", "")
					.add("__EVENTTARGET", "").add("__EVENTARGUMENT", "").add("__LASTFOCUS", "")
					.add("__VIEWSTATEGENERATOR", metadatos.get("__VIEWSTATEGENERATOR")).add("__VIEWSTATEENCRYPTED", "")
					.add("__ASYNCPOST", "true")
					// .add("__CSRFTOKEN", CSRFTOKEN)
					.add("ctl00$MainContent$BtnBusqueda", "Buscar CFDI")
					.add("__VIEWSTATE", metadatos.get("__VIEWSTATE")).build();

			Request request6 = new Request.Builder()
					.url("https://portalcfdi.facturaelectronica.sat.gob.mx/ConsultaReceptor.aspx")
					.method("POST", requestBody6).addHeader("X-MicrosoftAjax", "Delta=true")
					.addHeader("X-Requested-With", "XMLHttpRequest")
					.addHeader("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36")
					.build();

			Response response6 = client.newCall(request6).execute();

			String contenido6 = response6.body().string();

			String cookies_ok = String.join(";", response6.headers().values("Set-Cookie"));

			return SolicitudHTTP.builder().cookies(cookies_ok).resultados(parseResultados(contenido6)).build();

		} catch (IOException e) {

			e.printStackTrace();
		}
		return null;

	}

	public SolicitudHTTP buscarEmitidas(String cookies, String fecha_inicial, String fecha_final, String rfc_receptor,
			EstadoComprobante estadoComprobante) {

		try {

			System.out.println(fecha_inicial.substring(fecha_inicial.lastIndexOf('/') + 1));

			if (rfc_receptor == null)
				rfc_receptor = "";

			SolicitudesSAT solicitudes = new SolicitudesSAT();

			CookieManager cookieManager = new CookieManager();
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

			OkHttpClient client = new OkHttpClient().newBuilder().cookieJar(new JavaNetCookieJar(cookieManager))
					.connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
					.writeTimeout(30, TimeUnit.SECONDS).build();

			String view_state = solicitudes.obtenerViewStateInicialGet(SAT_CONSULTA_EMISOR, client, cookies);
			if (view_state == null)
				return null;

			HashMap<String, String> metadatos = solicitudes.obtenerViewStateAJAXPost(SAT_CONSULTA_EMISOR, client,
					view_state);
			if (metadatos == null)
				return null;

			RequestBody requestBody6 = new FormBody.Builder()

					.add("ctl00$ScriptManager1", "ctl00$MainContent$UpnlBusqueda|ctl00$MainContent$BtnBusqueda")
					.add("ctl00$MainContent$TxtUUID", "").add("ctl00$MainContent$FiltroCentral", "RdoFechas")

					.add("ctl00$MainContent$hfInicial", fecha_inicial.substring(fecha_inicial.lastIndexOf('/') + 1))
					.add("ctl00$MainContent$CldFechaInicial2$Calendario_text", fecha_inicial)
					.add("ctl00$MainContent$CldFechaInicial2$DdlHora", "0")
					.add("ctl00$MainContent$CldFechaInicial2$DdlMinuto", "0")
					.add("ctl00$MainContent$CldFechaInicial2$DdlSegundo", "0")

					.add("ctl00$MainContent$hfFinal", fecha_final.substring(fecha_final.lastIndexOf('/') + 1))
					.add("ctl00$MainContent$CldFechaFinal2$Calendario_text", fecha_final)
					.add("ctl00$MainContent$CldFechaFinal2$DdlHora", "23")
					.add("ctl00$MainContent$CldFechaFinal2$DdlMinuto", "59")
					.add("ctl00$MainContent$CldFechaFinal2$DdlSegundo", "59")

					.add("ctl00$MainContent$TxtRfcReceptor", rfc_receptor)
					.add("ctl00$MainContent$DdlEstadoComprobante", estadoComprobante.getEstado())
					.add("ctl00$MainContent$ddlComplementos", "-1")

					.add("ctl00$MainContent$ddlVigente", "0").add("ctl00$MainContent$ddlCancelado", "0")
					.add("ctl00$MainContent$hfDatos", "").add("ctl00$MainContent$hfFlag", "")
					.add("ctl00$MainContent$hfAux", "")

					.add("__EVENTTARGET", "").add("__EVENTARGUMENT", "").add("__LASTFOCUS", "")
					.add("__VIEWSTATEGENERATOR", metadatos.get("__VIEWSTATEGENERATOR")).add("__VIEWSTATEENCRYPTED", "")
					.add("__ASYNCPOST", "true").add("ctl00$MainContent$BtnBusqueda", "Buscar CFDI")
					.add("__VIEWSTATE", metadatos.get("__VIEWSTATE")).build();

			Request request6 = new Request.Builder().url(SAT_CONSULTA_EMISOR).method("POST", requestBody6)
					.addHeader("X-MicrosoftAjax", "Delta=true").addHeader("X-Requested-With", "XMLHttpRequest")
					.addHeader("User-Agent",
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36")
					.build();

			Response response6 = client.newCall(request6).execute();

			String contenido6 = response6.body().string();

			String cookies_ok = String.join(";", response6.headers().values("Set-Cookie"));

			return SolicitudHTTP.builder().cookies(cookies_ok).resultados(parseResultados(contenido6)).build();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public boolean verificaSesion(String cookies) throws SocketTimeoutException {
		try {
			OkHttpClient client = new OkHttpClient().newBuilder().build();

			Request request_PASO4 = new Request.Builder().url("https://portalcfdi.facturaelectronica.sat.gob.mx")
					.method("GET", null).addHeader("Cookie", cookies).build();

			Response response4 = client.newCall(request_PASO4).execute();

			String contenido4 = response4.body().string();

			if (contenido4.contains("Consultar Facturas Recibidas"))
				return true;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public ArrayList<Resultado> parseResultados(String html) {

		ArrayList<Resultado> resultados = new ArrayList<Resultado>();

		Document doc = Jsoup.parse(html, "UTF-8");

		Element tabla = doc.select("table#ctl00_MainContent_tblResult tbody").first();
		tabla.child(0).remove();

		for (Element fila : tabla.children()) {

			Elements s = fila.children();

			String estatusCancelacion = "";
			if (s.get(11).toString().contains("No cancelable"))
				estatusCancelacion = "No cancelable";
			else
				estatusCancelacion = s.get(10).select("span").text();

			resultados.add(new Resultado(s.get(1).select("span").text(), s.get(2).select("span").text(),
					s.get(3).select("span").text(), s.get(4).select("span").text(), s.get(5).select("span").text(),
					s.get(6).select("span").text(), s.get(7).select("span").text(), s.get(8).select("span").text(),
					s.get(9).select("span").text(), s.get(10).select("span").text(), estatusCancelacion,
					s.get(12).select("span").text(), s.get(13).select("span").text(), s.get(14).select("span").text()));
		}

		return resultados;

	}

}
