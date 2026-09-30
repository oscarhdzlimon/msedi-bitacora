package mx.gob.imss.edi.bitacora.models.dto;

import java.time.OffsetDateTime;

public class EventoBitacoraRegistro {

    private Long idEventoBitacora;
    private Long idEvento;
    private Long idMensaje;
    private Long idTransaccion;
    private Long idSistemaOrigen;
    private String refNombreUsuario;
    private String refError;
    private String refDetalle;
    private String refSesion;
    private String refTerminal;
    private String refObjeto;
    private String cveFolioIncapacidad;
    private String cveOperacion;
    private String refResultado;
    private OffsetDateTime stpOcurrencia;
    private String cveUsuarioAlta;

    public Long getIdEventoBitacora() { return idEventoBitacora; }
    public void setIdEventoBitacora(Long value) { this.idEventoBitacora = value; }
    public Long getIdEvento() { return idEvento; }
    public void setIdEvento(Long value) { this.idEvento = value; }
    public Long getIdMensaje() { return idMensaje; }
    public void setIdMensaje(Long value) { this.idMensaje = value; }
    public Long getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(Long value) { this.idTransaccion = value; }
    public Long getIdSistemaOrigen() { return idSistemaOrigen; }
    public void setIdSistemaOrigen(Long value) { this.idSistemaOrigen = value; }
    public String getRefNombreUsuario() { return refNombreUsuario; }
    public void setRefNombreUsuario(String value) { this.refNombreUsuario = value; }
    public String getRefError() { return refError; }
    public void setRefError(String value) { this.refError = value; }
    public String getRefDetalle() { return refDetalle; }
    public void setRefDetalle(String value) { this.refDetalle = value; }
    public String getRefSesion() { return refSesion; }
    public void setRefSesion(String value) { this.refSesion = value; }
    public String getRefTerminal() { return refTerminal; }
    public void setRefTerminal(String value) { this.refTerminal = value; }
    public String getRefObjeto() { return refObjeto; }
    public void setRefObjeto(String value) { this.refObjeto = value; }
    public String getCveFolioIncapacidad() { return cveFolioIncapacidad; }
    public void setCveFolioIncapacidad(String value) { this.cveFolioIncapacidad = value; }
    public String getCveOperacion() { return cveOperacion; }
    public void setCveOperacion(String value) { this.cveOperacion = value; }
    public String getRefResultado() { return refResultado; }
    public void setRefResultado(String value) { this.refResultado = value; }
    public OffsetDateTime getStpOcurrencia() { return stpOcurrencia; }
    public void setStpOcurrencia(OffsetDateTime value) { this.stpOcurrencia = value; }
    public String getCveUsuarioAlta() { return cveUsuarioAlta; }
    public void setCveUsuarioAlta(String value) { this.cveUsuarioAlta = value; }
}
