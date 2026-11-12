package com.municipio.base_conocimiento.seguridad.entity;

import com.municipio.base_conocimiento.compartido.persistence.EntidadAuditable;
import jakarta.persistence.*;

@Entity @Table(name="usuarios")
public class Usuario extends EntidadAuditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=120) private String nombre;
    @Column(nullable=false,length=60) private String usuario;
    @Column(name="usuario_normalizado",nullable=false,unique=true,length=60) private String usuarioNormalizado;
    @Column(length=180) private String email;
    @Column(name="email_normalizado",unique=true,length=180) private String emailNormalizado;
    @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
    @Column(name="password_temporal",nullable=false) private boolean passwordTemporal;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Rol rol;
    @Column(nullable=false) private boolean activo=true;
    protected Usuario() {}
    public Usuario(String nombre,String usuario,String usuarioNormalizado,String email,String emailNormalizado,String passwordHash,Rol rol){this(nombre,usuario,usuarioNormalizado,email,emailNormalizado,passwordHash,rol,false);}
    public Usuario(String nombre,String usuario,String usuarioNormalizado,String email,String emailNormalizado,String passwordHash,Rol rol,boolean passwordTemporal){this.nombre=nombre.strip();this.usuario=usuario.strip();this.usuarioNormalizado=usuarioNormalizado;this.email=limpiarEmail(email);this.emailNormalizado=emailNormalizado;this.passwordHash=passwordHash;this.rol=rol;this.passwordTemporal=passwordTemporal;}
    public void actualizar(String nombre,String usuario,String usuarioNormalizado,String email,String emailNormalizado,Rol rol,boolean activo,boolean passwordTemporal){this.nombre=nombre.strip();this.usuario=usuario.strip();this.usuarioNormalizado=usuarioNormalizado;this.email=limpiarEmail(email);this.emailNormalizado=emailNormalizado;this.rol=rol;this.activo=activo;this.passwordTemporal=passwordTemporal;}
    public void actualizarPerfil(String nombre,String email,String emailNormalizado){this.nombre=nombre.strip();this.email=limpiarEmail(email);this.emailNormalizado=emailNormalizado;}
    public void cambiarPassword(String hash){this.passwordHash=hash;this.passwordTemporal=false;}
    public void resetearPassword(String hash, boolean temporal){this.passwordHash=hash;this.passwordTemporal=temporal;}
    private static String limpiarEmail(String email){return email==null||email.isBlank()?null:email.strip();}
    public Long getId(){return id;} public String getNombre(){return nombre;} public String getUsuario(){return usuario;} public String getUsuarioNormalizado(){return usuarioNormalizado;} public String getEmail(){return email;} public String getEmailNormalizado(){return emailNormalizado;} public String getPasswordHash(){return passwordHash;} public boolean isPasswordTemporal(){return passwordTemporal;} public Rol getRol(){return rol;} public boolean isActivo(){return activo;}
}
