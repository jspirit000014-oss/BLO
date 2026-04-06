#!/usr/bin/env python3
"""
Stay Focused - Aplicación de Control de Tiempo de Pantalla
Inspirada en Stay Focused, compatible con Android (Pydroid/Buildozer)
"""

from kivy.app import App
from kivy.uix.screenmanager import ScreenManager, Screen
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.textinput import TextInput
from kivy.uix.spinner import Spinner
from kivy.uix.popup import Popup
from kivy.uix.progressbar import ProgressBar
from kivy.uix.switch import Switch
from kivy.graphics import Color, Rectangle, RoundedRectangle
from kivy.metrics import dp
from datetime import datetime
import json
import os
from typing import Dict
import threading
import time

# Detectar si estamos en Android (Buildozer/p4a)
try:
    from android.permissions import request_permissions, Permission
    ANDROID = True
except ImportError:
    ANDROID = False


class BaseDatos:
    """Gestor de base de datos local JSON - en Android usa directorio interno"""
    
    def __init__(self, archivo=None):
        if archivo is None:
            if ANDROID:
                try:
                    from jnius import autoclass
                    PythonActivity = autoclass('org.kivy.android.PythonActivity')
                    ctx = PythonActivity.mActivity
                    files_dir = ctx.getFilesDir().getAbsolutePath()
                    archivo = os.path.join(str(files_dir), "datos_app.json")
                except Exception:
                    archivo = "datos_app.json"
            else:
                archivo = "datos_app.json"
        self.archivo = archivo
        self.datos = self.cargar_datos()
    
    def cargar_datos(self) -> Dict:
        """Carga datos desde archivo JSON"""
        if os.path.exists(self.archivo):
            try:
                with open(self.archivo, 'r', encoding='utf-8') as f:
                    datos = json.load(f)
                    return self.validar_estructura(datos)
            except Exception:
                return self.datos_default()
        return self.datos_default()
    
    def validar_estructura(self, datos: Dict) -> Dict:
        """Valida y agrega campos faltantes"""
        if "perfiles" not in datos:
            datos["perfiles"] = []
        if "horarios_bloqueo" not in datos or not isinstance(datos["horarios_bloqueo"], list):
            datos["horarios_bloqueo"] = []
        horarios_validos = [h for h in datos["horarios_bloqueo"] if isinstance(h, dict)]
        datos["horarios_bloqueo"] = horarios_validos
        if "configuracion" not in datos:
            datos["configuracion"] = {}
        
        campos_default = {
            "modo_actual": "normal", "password_modo_bloqueo": "", "modo_estricto": False,
            "notificaciones": True, "tema_oscuro": False, "pausas_automaticas": True,
            "intervalo_pausa": 25, "stay_focused_activo": True, "rotacion_pantalla": False,
            "idioma": "Español", "cita_motivacional": False, "url_redireccion": "",
            "inicio_dia": "00:00", "duracion_pausa_habilitada": False, "bloquear_navegadores": False,
            "bloquear_pantalla_dividida": False, "bloquear_apagado": False,
            "bloquear_apps_recientes": False, "tiempo_bloqueo_pantalla": "1 minuto",
            "tipo_temporizador": "Temporizador en pantalla", "alertas_tiempo_bajo": [],
            "pomodoro_trabajo": 25, "pomodoro_descanso": 5, "pomodoro_activo": False,
            "modo_oscuro_activo": False
        }
        for campo, valor in campos_default.items():
            if campo not in datos["configuracion"]:
                datos["configuracion"][campo] = valor
        return datos
    
    def datos_default(self) -> Dict:
        return {
            "perfil": {"nombre": "Usuario", "edad": 25, "objetivo_diario": 120},
            "aplicaciones_bloqueadas": [], "horarios_bloqueo": [], "estadisticas_uso": [],
            "modos_configurados": [], "limites_diarios": {}, "historial_desbloqueos": [],
            "perfiles": [], "configuracion": {
                "modo_estricto": False, "modo_actual": "normal", "password_modo_bloqueo": "",
                "notificaciones": True, "tema_oscuro": False, "pausas_automaticas": True,
                "intervalo_pausa": 25, "stay_focused_activo": True, "rotacion_pantalla": False,
                "idioma": "Español", "cita_motivacional": False, "url_redireccion": "",
                "inicio_dia": "00:00", "duracion_pausa_habilitada": False,
                "bloquear_navegadores": False, "bloquear_pantalla_dividida": False,
                "bloquear_apagado": False, "bloquear_apps_recientes": False,
                "tiempo_bloqueo_pantalla": "1 minuto", "tipo_temporizador": "Temporizador en pantalla",
                "alertas_tiempo_bajo": [], "pomodoro_trabajo": 25, "pomodoro_descanso": 5,
                "pomodoro_activo": False, "modo_oscuro_activo": False
            }
        }
    
    def guardar_datos(self):
        try:
            with open(self.archivo, 'w', encoding='utf-8') as f:
                json.dump(self.datos, f, indent=2, ensure_ascii=False)
            return True
        except Exception as e:
            print(f"Error al guardar: {e}")
            return False
    
    def agregar_estadistica(self, app_nombre: str, minutos: int):
        self.datos["estadisticas_uso"].append({
            "fecha": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "app": app_nombre, "minutos": minutos
        })
        self.guardar_datos()
    
    def obtener_estadisticas_hoy(self) -> Dict:
        hoy = datetime.now().strftime("%Y-%m-%d")
        stats = {}
        for r in self.datos["estadisticas_uso"]:
            if r["fecha"].startswith(hoy):
                app = r["app"]
                stats[app] = stats.get(app, 0) + r["minutos"]
        return stats


class GestorTemporizadores:
    def __init__(self, callback_actualizar=None):
        self.temporizadores_activos = {}
        self.callback_actualizar = callback_actualizar
        self.corriendo = False
        self._hilo = None
    
    def iniciar_temporizador(self, nombre: str, minutos: int, callback=None):
        self.temporizadores_activos[nombre] = {
            "inicio": time.time(), "duracion": minutos * 60, "callback": callback
        }
        if not self.corriendo:
            self.corriendo = True
            self._hilo = threading.Thread(target=self._ejecutar, daemon=True)
            self._hilo.start()
    
    def _ejecutar(self):
        while self.corriendo and self.temporizadores_activos:
            t = time.time()
            for nombre, datos in list(self.temporizadores_activos.items()):
                if t - datos["inicio"] >= datos["duracion"]:
                    del self.temporizadores_activos[nombre]
                    if datos["callback"]:
                        datos["callback"](nombre)
            if self.callback_actualizar:
                self.callback_actualizar()
            time.sleep(1)
        self.corriendo = False
    
    def detener_temporizador(self, nombre: str):
        if nombre in self.temporizadores_activos:
            del self.temporizadores_activos[nombre]
    
    def obtener_tiempo_restante(self, nombre: str) -> int:
        if nombre in self.temporizadores_activos:
            d = self.temporizadores_activos[nombre]
            r = d["duracion"] - (time.time() - d["inicio"])
            return max(0, int(r))
        return 0


class ColoredButton(Button):
    def __init__(self, bg_color=(0.22, 0, 0.7, 1), **kwargs):
        super().__init__(**kwargs)
        self.background_normal = ''
        self.background_color = bg_color
        self.color = (1, 1, 1, 1)
        self.size_hint_y = None
        self.height = dp(50)


class TarjetaApp(BoxLayout):
    def __init__(self, nombre, minutos, limite=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = 'vertical'
        self.size_hint_y = None
        self.height = dp(120)
        self.padding = dp(10)
        self.spacing = dp(5)
        with self.canvas.before:
            Color(0.18, 0.18, 0.27, 1)
            self.rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(10)])
        self.bind(pos=self._upd, size=self._upd)
        self.add_widget(Label(text=f"[b]{nombre}[/b]", markup=True, font_size='18sp',
                             size_hint_y=0.3, color=(0.73, 0.53, 0.99, 1)))
        self.add_widget(Label(text=f"Tiempo: {minutos} minutos", font_size='14sp',
                             size_hint_y=0.3, color=(1, 1, 1, 1)))
        if limite:
            pct = min(minutos / limite, 1.0) * 100
            self.add_widget(ProgressBar(max=100, value=pct, size_hint_y=0.4))
        else:
            self.add_widget(Label(text="Sin límite", size_hint_y=0.4))
    
    def _upd(self, *args):
        self.rect.pos, self.rect.size = self.pos, self.size


class PantallaInicio(Screen):
    def __init__(self, app_instance, **kwargs):
        super().__init__(**kwargs)
        self.name = 'inicio'
        self.app_instance = app_instance
        self.db = app_instance.db
        with self.canvas.before:
            Color(0.12, 0.12, 0.18, 1)
            self.rect = Rectangle(pos=self.pos, size=self.size)
        self.bind(pos=self._upd, size=self._upd)
        self.crear_interfaz()
    
    def _upd(self, *args):
        self.rect.pos, self.rect.size = self.pos, self.size
    
    def crear_interfaz(self):
        layout = BoxLayout(orientation='vertical', padding=dp(15), spacing=dp(15))
        layout.add_widget(Label(text="Tómate Un Descanso", font_size='28sp', bold=True,
                               size_hint_y=None, height=dp(50), color=(0.01, 0.85, 0.78, 1)))
        layout.add_widget(Label(text="Tómate un descanso de tu teléfono y concéntrate en lo que realmente importa.",
                               font_size='14sp', size_hint_y=None, height=dp(40),
                               color=(0.7, 0.7, 0.7, 1), text_size=(dp(350), None)))
        
        scroll = ScrollView()
        cont = BoxLayout(orientation='vertical', spacing=dp(15), size_hint_y=None)
        cont.bind(minimum_height=cont.setter('height'))
        
        cont.add_widget(Label(text="Nivel de estrictitud", font_size='18sp', bold=True,
                             size_hint_y=None, height=dp(40)))
        self._crear_tarjeta_modos(cont)
        
        cont.add_widget(Label(text="Ajustes Rápidos", font_size='18sp', bold=True,
                             size_hint_y=None, height=dp(40)))
        apps_count = len(self.db.datos["aplicaciones_bloqueadas"])
        self._crear_ajuste_contador(cont, "Aplicaciones Bloqueadas", apps_count)
        self._crear_ajuste_contador(cont, "Sitios Bloqueados", 0)
        self._crear_ajuste_configurar(cont, "Palabras Clave Bloqueadas", "Apagado")
        self._crear_ajuste_configurar(cont, "Bloquear Contenido Para Adultos", "Apagado")
        self._crear_ajuste_configurar(cont, "Bloquear Reels/Shorts", "Apagado")
        
        cont.add_widget(Label(text="Perfiles", font_size='18sp', bold=True,
                             size_hint_y=None, height=dp(40)))
        btn = ColoredButton(text="AGREGAR UN NUEVO PERFIL", bg_color=(0.22, 0, 0.7, 1))
        btn.bind(on_press=self.agregar_perfil)
        cont.add_widget(btn)
        
        cont.add_widget(Label(text="[b]⚡ Acceso Rápido[/b]", markup=True, font_size='18sp',
                             size_hint_y=None, height=dp(40), color=(1, 1, 1, 1)))
        acciones = [
            ("🚫 Bloquear Apps", lambda: setattr(self.manager, 'current', 'bloqueo')),
            ("⏰ Crear Horario", lambda: setattr(self.manager, 'current', 'horarios')),
            ("📊 Ver Estadísticas", lambda: setattr(self.manager, 'current', 'estadisticas')),
            ("⚙️ Configuración", lambda: setattr(self.manager, 'current', 'configuracion')),
            ("🎯 Establecer Meta", self.establecer_meta),
            ("🔓 Gestionar Bloqueos", self.gestionar_bloqueos)
        ]
        for texto, cmd in acciones:
            b = ColoredButton(text=texto)
            b.bind(on_press=lambda x, c=cmd: c())
            cont.add_widget(b)
        
        cont.add_widget(Label(text="[b]📈 Resumen del Día[/b]", markup=True, font_size='18sp',
                             size_hint_y=None, height=dp(40), color=(1, 1, 1, 1)))
        stats = self.db.obtener_estadisticas_hoy()
        if stats:
            for app, mins in sorted(stats.items(), key=lambda x: -x[1])[:5]:
                cont.add_widget(TarjetaApp(app, mins, self.db.datos["limites_diarios"].get(app)))
        else:
            cont.add_widget(Label(text="No hay estadísticas para hoy", size_hint_y=None,
                                 height=dp(50), color=(0.7, 0.7, 0.7, 1)))
        
        scroll.add_widget(cont)
        layout.add_widget(scroll)
        self.add_widget(layout)
    
    def _crear_tarjeta_modos(self, c):
        tarjeta = BoxLayout(orientation='vertical', spacing=dp(10), size_hint_y=None,
                           padding=dp(15), height=dp(380))
        with tarjeta.canvas.before:
            Color(0.18, 0.18, 0.27, 1)
            r = RoundedRectangle(pos=tarjeta.pos, size=tarjeta.size, radius=[dp(10)])
        tarjeta.bind(pos=lambda *a: setattr(r, 'pos', tarjeta.pos),
                     size=lambda *a: setattr(r, 'size', tarjeta.size))
        modo = self.db.datos["configuracion"]["modo_actual"]
        self._crear_opcion_modo(tarjeta, "Activo" if modo == "normal" else "",
                               "Modo normal", "Puedes cambiar la configuración libremente.", "normal", modo == "normal")
        self._crear_opcion_modo(tarjeta, "", "Modo de bloqueo",
                               "Contraseña para bloquear la configuración.", "bloqueo", modo == "bloqueo")
        self._crear_opcion_modo_estricto(tarjeta, "Modo estricto",
                                         "Evita cambios y desinstalación.", "estricto", modo == "estricto")
        c.add_widget(tarjeta)
    
    def _crear_opcion_modo(self, c, badge, titulo, desc, modo, activo):
        op = BoxLayout(orientation='vertical', spacing=dp(5), size_hint_y=None, height=dp(110))
        if badge:
            op.add_widget(Label(text=badge, font_size='11sp', size_hint_y=None, height=dp(20),
                               color=(0.01, 0.85, 0.78, 1)))
        op.add_widget(Label(text=titulo, font_size='16sp', bold=True, size_hint_y=None, height=dp(25),
                           color=(1, 1, 1, 1) if activo else (0.8, 0.8, 0.8, 1)))
        op.add_widget(Label(text=desc, font_size='12sp', size_hint_y=None, height=dp(40),
                           text_size=(dp(250), None), color=(0.7, 0.7, 0.7, 1)))
        if not activo:
            b = ColoredButton(text="Activar", bg_color=(0.22, 0, 0.7, 1), height=dp(35))
            b.bind(on_press=lambda x: self.activar_modo(modo))
            op.add_widget(b)
        c.add_widget(op)
    
    def _crear_opcion_modo_estricto(self, c, titulo, desc, modo, activo):
        op = BoxLayout(orientation='vertical', spacing=dp(5), size_hint_y=None, height=dp(110))
        op.add_widget(Label(text=titulo, font_size='16sp', bold=True, size_hint_y=None, height=dp(25),
                           color=(1, 1, 1, 1) if activo else (0.8, 0.8, 0.8, 1)))
        op.add_widget(Label(text=desc, font_size='12sp', size_hint_y=None, height=dp(50),
                           text_size=(dp(250), None), color=(0.7, 0.7, 0.7, 1)))
        if not activo:
            b = ColoredButton(text="ACTIVAR CON LA ÚLTIMA CONFIGURACIÓN",
                              bg_color=(0.69, 0, 0.13, 1), height=dp(35))
            b.bind(on_press=lambda x: self.activar_modo_estricto())
            op.add_widget(b)
        c.add_widget(op)
    
    def _crear_ajuste_contador(self, c, titulo, valor):
        a = BoxLayout(orientation='horizontal', size_hint_y=None, height=dp(50), padding=dp(10))
        with a.canvas.before:
            Color(0.18, 0.18, 0.27, 1)
            r = RoundedRectangle(pos=a.pos, size=a.size, radius=[dp(8)])
        a.bind(pos=lambda *x: setattr(r, 'pos', a.pos), size=lambda *x: setattr(r, 'size', a.size))
        a.add_widget(Label(text=titulo, font_size='14sp'))
        a.add_widget(Label(text=str(valor), font_size='16sp', bold=True, size_hint_x=None,
                          width=dp(50), color=(0.01, 0.85, 0.78, 1)))
        c.add_widget(a)
    
    def _crear_ajuste_configurar(self, c, titulo, estado):
        a = BoxLayout(orientation='vertical', size_hint_y=None, height=dp(70), padding=dp(10), spacing=dp(5))
        with a.canvas.before:
            Color(0.18, 0.18, 0.27, 1)
            r = RoundedRectangle(pos=a.pos, size=a.size, radius=[dp(8)])
        a.bind(pos=lambda *x: setattr(r, 'pos', a.pos), size=lambda *x: setattr(r, 'size', a.size))
        f = BoxLayout(orientation='horizontal', size_hint_y=None, height=dp(30))
        f.add_widget(Label(text=titulo, font_size='14sp'))
        f.add_widget(Label(text=estado, font_size='12sp', size_hint_x=None, width=dp(70),
                          color=(0.7, 0.7, 0.7, 1)))
        a.add_widget(f)
        btn = Button(text="Configurar", size_hint_y=None, height=dp(30), background_normal='',
                     background_color=(0.22, 0, 0.7, 1), color=(1, 1, 1, 1))
        btn.bind(on_press=lambda x: self._msg(f"Configuración: {titulo}\n(En desarrollo)"))
        a.add_widget(btn)
        c.add_widget(a)
    
    def activar_modo(self, modo):
        if modo == "bloqueo":
            self._solicitar_password()
        else:
            self.db.datos["configuracion"]["modo_actual"] = modo
            self.db.guardar_datos()
            self._msg(f"Modo {modo} activado")
            self.clear_widgets()
            self.crear_interfaz()
    
    def _solicitar_password(self):
        cnt = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        cnt.add_widget(Label(text="Establece contraseña para Modo de Bloqueo:"))
        pw = TextInput(hint_text="Contraseña", password=True, multiline=False, size_hint_y=None, height=dp(40))
        pw2 = TextInput(hint_text="Confirmar", password=True, multiline=False, size_hint_y=None, height=dp(40))
        cnt.add_widget(pw)
        cnt.add_widget(pw2)
        btns = BoxLayout(spacing=dp(10), size_hint_y=None, height=dp(50))
        pop = Popup(title="Modo de Bloqueo", content=cnt, size_hint=(0.9, 0.5))
        def ok(_):
            if pw.text and pw.text == pw2.text:
                self.db.datos["configuracion"]["password_modo_bloqueo"] = pw.text
                self.db.datos["configuracion"]["modo_actual"] = "bloqueo"
                self.db.guardar_datos()
                pop.dismiss()
                self._msg("Modo de bloqueo activado")
                self.clear_widgets()
                self.crear_interfaz()
            else:
                self._msg("Las contraseñas no coinciden", error=True)
        b_ok = ColoredButton(text="Activar", bg_color=(0.01, 0.85, 0.78, 1))
        b_ok.bind(on_press=ok)
        b_cancel = ColoredButton(text="Cancelar", bg_color=(0.18, 0.18, 0.27, 1))
        b_cancel.bind(on_press=pop.dismiss)
        btns.add_widget(b_ok)
        btns.add_widget(b_cancel)
        cnt.add_widget(btns)
        pop.open()
    
    def activar_modo_estricto(self):
        cnt = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        cnt.add_widget(Label(text="⚠️ El Modo Estricto impedirá cambios.\n¿Continuar?"))
        btns = BoxLayout(spacing=dp(10), size_hint_y=None, height=dp(50))
        pop = Popup(title="Modo Estricto", content=cnt, size_hint=(0.9, 0.5))
        def ok(_):
            self.db.datos["configuracion"]["modo_actual"] = "estricto"
            self.db.datos["configuracion"]["modo_estricto"] = True
            self.db.guardar_datos()
            pop.dismiss()
            self._msg("Modo Estricto activado")
            self.clear_widgets()
            self.crear_interfaz()
        b_ok = ColoredButton(text="Sí, activar", bg_color=(0.69, 0, 0.13, 1))
        b_ok.bind(on_press=ok)
        b_cancel = ColoredButton(text="Cancelar", bg_color=(0.18, 0.18, 0.27, 1))
        b_cancel.bind(on_press=pop.dismiss)
        btns.add_widget(b_ok)
        btns.add_widget(b_cancel)
        cnt.add_widget(btns)
        pop.open()
    
    def agregar_perfil(self, inst):
        cnt = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        cnt.add_widget(Label(text="Nombre del perfil:"))
        inp = TextInput(hint_text="Ej: Trabajo, Estudio", multiline=False, size_hint_y=None, height=dp(40))
        cnt.add_widget(inp)
        btns = BoxLayout(spacing=dp(10), size_hint_y=None, height=dp(50))
        pop = Popup(title="Nuevo Perfil", content=cnt, size_hint=(0.8, 0.4))
        def guardar(_):
            if inp.text.strip():
                self.db.datos["perfiles"].append({
                    "nombre": inp.text.strip(), "apps_bloqueadas": [], "horarios": [],
                    "creado": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                })
                self.db.guardar_datos()
                pop.dismiss()
                self._msg(f"Perfil '{inp.text}' creado")
            else:
                self._msg("Ingresa un nombre válido", error=True)
        b_ok = ColoredButton(text="Crear", bg_color=(0.01, 0.85, 0.78, 1))
        b_ok.bind(on_press=guardar)
        b_cancel = ColoredButton(text="Cancelar", bg_color=(0.18, 0.18, 0.27, 1))
        b_cancel.bind(on_press=pop.dismiss)
        btns.add_widget(b_ok)
        btns.add_widget(b_cancel)
        cnt.add_widget(btns)
        pop.open()
    
    def establecer_meta(self):
        cnt = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        cnt.add_widget(Label(text="Meta diaria (minutos):"))
        e = TextInput(text=str(self.db.datos["perfil"]["objetivo_diario"]),
                      multiline=False, input_filter='int', size_hint_y=None, height=dp(40))
        cnt.add_widget(e)
        pop = Popup(title="Establecer Meta", content=cnt, size_hint=(0.9, 0.4))
        def g(_):
            try:
                self.db.datos["perfil"]["objetivo_diario"] = int(e.text)
                self.db.guardar_datos()
                pop.dismiss()
                self._msg("Meta actualizada")
            except ValueError:
                self._msg("Número inválido", error=True)
        b = ColoredButton(text="Guardar", bg_color=(0.01, 0.85, 0.78, 1))
        b.bind(on_press=g)
        cnt.add_widget(b)
        pop.open()
    
    def gestionar_bloqueos(self):
        cnt = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        sv = ScrollView()
        lst = BoxLayout(orientation='vertical', size_hint_y=None, spacing=dp(5))
        lst.bind(minimum_height=lst.setter('height'))
        pop = Popup(title="Apps Bloqueadas", content=cnt, size_hint=(0.9, 0.7))
        for app in self.db.datos["aplicaciones_bloqueadas"]:
            it = BoxLayout(size_hint_y=None, height=dp(50))
            it.add_widget(Label(text=app))
            b = ColoredButton(text="Eliminar", bg_color=(0.69, 0.4, 0.47, 1), size_hint_x=0.3)
            def elim_and_close(_, a=app):
                self._eliminar_bloqueo(a)
                pop.dismiss()
                self.clear_widgets()
                self.crear_interfaz()
            b.bind(on_press=elim_and_close)
            it.add_widget(b)
            lst.add_widget(it)
        if not self.db.datos["aplicaciones_bloqueadas"]:
            lst.add_widget(Label(text="No hay apps bloqueadas", size_hint_y=None, height=dp(50)))
        sv.add_widget(lst)
        cnt.add_widget(sv)
        pop.open()
    
    def _eliminar_bloqueo(self, app):
        if app in self.db.datos["aplicaciones_bloqueadas"]:
            self.db.datos["aplicaciones_bloqueadas"].remove(app)
            self.db.guardar_datos()
            self._msg(f"{app} desbloqueada")
    
    def _msg(self, mensaje, error=False):
        col = (0.69, 0, 0.13, 1) if error else (0.01, 0.85, 0.78, 1)
        c = BoxLayout(orientation='vertical', padding=dp(10))
        c.add_widget(Label(text=mensaje))
        b = ColoredButton(text="OK", bg_color=col)
        p = Popup(title="Aviso", content=c, size_hint=(0.8, 0.3))
        b.bind(on_press=p.dismiss)
        c.add_widget(b)
        p.open()


class PantallaBloqueoApps(Screen):
    def __init__(self, app_instance, **kwargs):
        super().__init__(**kwargs)
        self.name = 'bloqueo'
        self.app_instance = app_instance
        self.db = app_instance.db
        with self.canvas.before:
            Color(0.12, 0.12, 0.18, 1)
            self.rect = Rectangle(pos=self.pos, size=self.size)
        self.bind(pos=lambda *a: setattr(self.rect, 'pos', self.pos),
                 size=lambda *a: setattr(self.rect, 'size', self.size))
        self.crear_interfaz()
    
    def crear_interfaz(self):
        layout = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        h = BoxLayout(size_hint_y=0.1)
        bv = ColoredButton(text="← Volver", bg_color=(0.18, 0.18, 0.27, 1), size_hint_x=0.3)
        bv.bind(on_press=lambda x: setattr(self.manager, 'current', 'inicio'))
        h.add_widget(bv)
        h.add_widget(Label(text="[b]🚫 Bloquear Apps[/b]", markup=True, font_size='20sp',
                          color=(0.73, 0.53, 0.99, 1)))
        layout.add_widget(h)
        
        sv = ScrollView(size_hint=(1, 0.9))
        c = BoxLayout(orientation='vertical', size_hint_y=None, spacing=dp(10), padding=dp(10))
        c.bind(minimum_height=c.setter('height'))
        c.add_widget(Label(text="Nombre de la aplicación:", size_hint_y=None, height=dp(40), color=(1,1,1,1)))
        self.entrada_app = TextInput(hint_text="Ej: Instagram", multiline=False, size_hint_y=None, height=dp(50))
        c.add_widget(self.entrada_app)
        c.add_widget(Label(text="Duración (min, 0=permanente):", size_hint_y=None, height=dp(40), color=(1,1,1,1)))
        self.entrada_duracion = TextInput(text="60", multiline=False, input_filter='int', size_hint_y=None, height=dp(50))
        c.add_widget(self.entrada_duracion)
        c.add_widget(Label(text="Límite diario (min, 0=sin límite):", size_hint_y=None, height=dp(40), color=(1,1,1,1)))
        self.entrada_limite = TextInput(text="30", multiline=False, input_filter='int', size_hint_y=None, height=dp(50))
        c.add_widget(self.entrada_limite)
        bb = ColoredButton(text="🔒 Bloquear", bg_color=(0.69, 0, 0.13, 1))
        bb.bind(on_press=self.bloquear_app)
        c.add_widget(bb)
        c.add_widget(Label(text="[b]Apps bloqueadas:[/b]", markup=True, size_hint_y=None, height=dp(40), color=(1,1,1,1)))
        for app in self.db.datos["aplicaciones_bloqueadas"]:
            it = BoxLayout(size_hint_y=None, height=dp(50))
            it.add_widget(Label(text=f"🔒 {app}"))
            bd = ColoredButton(text="Desbloquear", bg_color=(0.01, 0.85, 0.78, 1), size_hint_x=0.3)
            bd.bind(on_press=lambda x, a=app: self.desbloquear_app(a))
            it.add_widget(bd)
            c.add_widget(it)
        if not self.db.datos["aplicaciones_bloqueadas"]:
            c.add_widget(Label(text="No hay apps bloqueadas", size_hint_y=None, height=dp(50), color=(0.7,0.7,0.7,1)))
        sv.add_widget(c)
        layout.add_widget(sv)
        self.add_widget(layout)
    
    def bloquear_app(self, inst):
        app = self.entrada_app.text.strip()
        if not app:
            self._msg("Ingresa nombre de app", error=True)
            return
        if app in self.db.datos["aplicaciones_bloqueadas"]:
            self._msg("Ya está bloqueada", error=True)
            return
        self.db.datos["aplicaciones_bloqueadas"].append(app)
        try:
            lim = int(self.entrada_limite.text)
            if lim > 0:
                self.db.datos["limites_diarios"][app] = lim
        except ValueError:
            pass
        self.db.guardar_datos()
        self._msg(f"{app} bloqueada")
        self.entrada_app.text = ""
        self.clear_widgets()
        self.crear_interfaz()
    
    def desbloquear_app(self, app):
        if app in self.db.datos["aplicaciones_bloqueadas"]:
            self.db.datos["aplicaciones_bloqueadas"].remove(app)
            self.db.guardar_datos()
            self._msg(f"{app} desbloqueada")
            self.clear_widgets()
            self.crear_interfaz()
    
    def _msg(self, m, error=False):
        col = (0.69, 0, 0.13, 1) if error else (0.01, 0.85, 0.78, 1)
        c = BoxLayout(orientation='vertical', padding=dp(10))
        c.add_widget(Label(text=m))
        b = ColoredButton(text="OK", bg_color=col)
        p = Popup(title="Aviso", content=c, size_hint=(0.8, 0.3))
        b.bind(on_press=p.dismiss)
        c.add_widget(b)
        p.open()


class PantallaHorarios(Screen):
    def __init__(self, app_instance, **kwargs):
        super().__init__(**kwargs)
        self.name = 'horarios'
        self.app_instance = app_instance
        self.db = app_instance.db
        with self.canvas.before:
            Color(0.12, 0.12, 0.18, 1)
            self.rect = Rectangle(pos=self.pos, size=self.size)
        self.bind(pos=lambda *a: setattr(self.rect, 'pos', self.pos),
                 size=lambda *a: setattr(self.rect, 'size', self.size))
        self.crear_interfaz()
    
    def crear_interfaz(self):
        layout = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        h = BoxLayout(size_hint_y=0.1)
        bv = ColoredButton(text="← Volver", bg_color=(0.18, 0.18, 0.27, 1), size_hint_x=0.3)
        bv.bind(on_press=lambda x: setattr(self.manager, 'current', 'inicio'))
        h.add_widget(bv)
        h.add_widget(Label(text="[b]⏰ Horarios[/b]", markup=True, font_size='20sp', color=(0.73, 0.53, 0.99, 1)))
        layout.add_widget(h)
        sv = ScrollView(size_hint=(1, 0.9))
        c = BoxLayout(orientation='vertical', size_hint_y=None, spacing=dp(10), padding=dp(10))
        c.bind(minimum_height=c.setter('height'))
        c.add_widget(Label(text="Nuevo Horario", font_size='18sp', bold=True, size_hint_y=None, height=dp(40), color=(1,1,1,1)))
        c.add_widget(Label(text="Inicio (HH:MM):", size_hint_y=None, height=dp(30), color=(1,1,1,1)))
        self.entrada_inicio = TextInput(hint_text="08:00", multiline=False, size_hint_y=None, height=dp(50))
        c.add_widget(self.entrada_inicio)
        c.add_widget(Label(text="Fin (HH:MM):", size_hint_y=None, height=dp(30), color=(1,1,1,1)))
        self.entrada_fin = TextInput(hint_text="17:00", multiline=False, size_hint_y=None, height=dp(50))
        c.add_widget(self.entrada_fin)
        self.spinner_dias = Spinner(text="Lunes a Viernes",
                                    values=("Lunes a Viernes", "Todos los días", "Fines de semana", "Personalizado"),
                                    size_hint_y=None, height=dp(50))
        c.add_widget(self.spinner_dias)
        ba = ColoredButton(text="+ Agregar", bg_color=(0.01, 0.85, 0.78, 1))
        ba.bind(on_press=self.agregar_horario)
        c.add_widget(ba)
        c.add_widget(Label(text="[b]Horarios:[/b]", markup=True, size_hint_y=None, height=dp(40), color=(1,1,1,1)))
        for h in self.db.datos["horarios_bloqueo"]:
            it = BoxLayout(size_hint_y=None, height=dp(70), padding=dp(5))
            with it.canvas.before:
                Color(0.18, 0.18, 0.27, 1)
                r = RoundedRectangle(pos=it.pos, size=it.size, radius=[dp(10)])
            it.bind(pos=lambda *a, rr=r: setattr(rr, 'pos', it.pos), size=lambda *a, rr=r: setattr(rr, 'size', it.size))
            info = BoxLayout(orientation='vertical')
            info.add_widget(Label(text=f"⏰ {h.get('inicio','')} - {h.get('fin','')}", font_size='14sp', bold=True))
            info.add_widget(Label(text=str(h.get('dias','')), font_size='12sp', color=(0.7,0.7,0.7,1)))
            it.add_widget(info)
            c.add_widget(it)
        if not self.db.datos["horarios_bloqueo"]:
            c.add_widget(Label(text="No hay horarios", size_hint_y=None, height=dp(50), color=(0.7,0.7,0.7,1)))
        sv.add_widget(c)
        layout.add_widget(sv)
        self.add_widget(layout)
    
    def agregar_horario(self, inst):
        i, f = self.entrada_inicio.text.strip(), self.entrada_fin.text.strip()
        if not i or not f:
            self._msg("Completa los campos", error=True)
            return
        self.db.datos["horarios_bloqueo"].append({"inicio": i, "fin": f, "dias": self.spinner_dias.text, "activo": True})
        self.db.guardar_datos()
        self._msg("Horario agregado")
        self.entrada_inicio.text = self.entrada_fin.text = ""
        self.clear_widgets()
        self.crear_interfaz()
    
    def _msg(self, m, error=False):
        col = (0.69, 0, 0.13, 1) if error else (0.01, 0.85, 0.78, 1)
        c = BoxLayout(orientation='vertical', padding=dp(10))
        c.add_widget(Label(text=m))
        b = ColoredButton(text="OK", bg_color=col)
        p = Popup(title="Aviso", content=c, size_hint=(0.8, 0.3))
        b.bind(on_press=p.dismiss)
        c.add_widget(b)
        p.open()


class PantallaEstadisticas(Screen):
    def __init__(self, app_instance, **kwargs):
        super().__init__(**kwargs)
        self.name = 'estadisticas'
        self.app_instance = app_instance
        self.db = app_instance.db
        with self.canvas.before:
            Color(0.12, 0.12, 0.18, 1)
            self.rect = Rectangle(pos=self.pos, size=self.size)
        self.bind(pos=lambda *a: setattr(self.rect, 'pos', self.pos),
                 size=lambda *a: setattr(self.rect, 'size', self.size))
        self.crear_interfaz()
    
    def crear_interfaz(self):
        layout = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        h = BoxLayout(size_hint_y=0.1)
        bv = ColoredButton(text="← Volver", bg_color=(0.18, 0.18, 0.27, 1), size_hint_x=0.3)
        bv.bind(on_press=lambda x: setattr(self.manager, 'current', 'inicio'))
        h.add_widget(bv)
        h.add_widget(Label(text="[b]📊 Estadísticas[/b]", markup=True, font_size='20sp', color=(0.73, 0.53, 0.99, 1)))
        layout.add_widget(h)
        sv = ScrollView(size_hint=(1, 0.9))
        c = BoxLayout(orientation='vertical', size_hint_y=None, spacing=dp(10), padding=dp(10))
        c.bind(minimum_height=c.setter('height'))
        stats = self.db.obtener_estadisticas_hoy()
        if stats:
            tot = sum(stats.values())
            obj = self.db.datos["perfil"]["objetivo_diario"]
            pct = min(tot / obj * 100, 100) if obj > 0 else 0
            res = BoxLayout(orientation='vertical', size_hint_y=None, height=dp(150), padding=dp(15), spacing=dp(10))
            with res.canvas.before:
                Color(0.22, 0, 0.7, 1)
                r = RoundedRectangle(pos=res.pos, size=res.size, radius=[dp(15)])
            res.bind(pos=lambda *a: setattr(r, 'pos', res.pos), size=lambda *a: setattr(r, 'size', res.size))
            res.add_widget(Label(text="Tiempo Total Hoy", font_size='16sp', size_hint_y=0.3, color=(1,1,1,1)))
            res.add_widget(Label(text=f"{tot} min", font_size='32sp', bold=True, size_hint_y=0.4, color=(0.01, 0.85, 0.78, 1)))
            res.add_widget(ProgressBar(max=100, value=pct, size_hint_y=0.2))
            res.add_widget(Label(text=f"Objetivo: {obj} min ({int(pct)}%)", font_size='14sp', size_hint_y=0.1, color=(0.9,0.9,0.9,1)))
            c.add_widget(res)
            c.add_widget(Label(text="[b]Apps hoy:[/b]", markup=True, size_hint_y=None, height=dp(40), color=(1,1,1,1)))
            for app, mins in sorted(stats.items(), key=lambda x: -x[1]):
                c.add_widget(TarjetaApp(app, mins, self.db.datos["limites_diarios"].get(app)))
        else:
            c.add_widget(Label(text="No hay estadísticas.\nUsa apps para generar datos.", font_size='16sp',
                              size_hint_y=None, height=dp(150), color=(0.7,0.7,0.7,1)))
        sv.add_widget(c)
        layout.add_widget(sv)
        self.add_widget(layout)


class PantallaConfiguracion(Screen):
    def __init__(self, app_instance, **kwargs):
        super().__init__(**kwargs)
        self.name = 'configuracion'
        self.app_instance = app_instance
        self.db = app_instance.db
        with self.canvas.before:
            Color(0.12, 0.12, 0.18, 1)
            self.rect = Rectangle(pos=self.pos, size=self.size)
        self.bind(pos=lambda *a: setattr(self.rect, 'pos', self.pos),
                 size=lambda *a: setattr(self.rect, 'size', self.size))
        self.crear_interfaz()
    
    def crear_interfaz(self):
        layout = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))
        h = BoxLayout(size_hint_y=0.1)
        bv = ColoredButton(text="← Volver", bg_color=(0.18, 0.18, 0.27, 1), size_hint_x=0.3)
        bv.bind(on_press=lambda x: setattr(self.manager, 'current', 'inicio'))
        h.add_widget(bv)
        h.add_widget(Label(text="[b]⚙️ Configuración[/b]", markup=True, font_size='20sp', color=(0.73, 0.53, 0.99, 1)))
        layout.add_widget(h)
        sv = ScrollView(size_hint=(1, 0.9))
        c = BoxLayout(orientation='vertical', size_hint_y=None, spacing=dp(10), padding=dp(10))
        c.bind(minimum_height=c.setter('height'))
        cfg = self.db.datos["configuracion"]
        
        for sec, opts in [
            ("GENERAL", [
                ("Pausar Stay Focused", "stay_focused_activo", "switch"),
                ("Permitir Rotación", "rotacion_pantalla", "switch"),
            ]),
            ("PANTALLA BLOQUEO", [
                ("Cita Motivacional", "cita_motivacional", "switch"),
            ]),
            ("POMODORO", [
                ("Activar Pomodoro", "pomodoro_activo", "switch"),
            ]),
            ("AVANZADO", [
                ("Bloquear Navegadores", "bloquear_navegadores", "switch"),
                ("Bloquear Pantalla Dividida", "bloquear_pantalla_dividida", "switch"),
                ("Bloquear Apagado", "bloquear_apagado", "switch"),
                ("Bloquear Apps Recientes", "bloquear_apps_recientes", "switch"),
            ]),
        ]:
            c.add_widget(Label(text=sec, font_size='14sp', bold=True, size_hint_y=None, height=dp(30),
                              color=(0.01, 0.85, 0.78, 1)))
            for titulo, key, typ in opts:
                if typ == "switch":
                    op = BoxLayout(orientation='horizontal', size_hint_y=None, height=dp(50), padding=dp(10))
                    with op.canvas.before:
                        Color(0.18, 0.18, 0.27, 1)
                        r = RoundedRectangle(pos=op.pos, size=op.size, radius=[dp(8)])
                    op.bind(pos=lambda *a, rr=r: setattr(rr, 'pos', op.pos),
                            size=lambda *a, rr=r: setattr(rr, 'size', op.size))
                    op.add_widget(Label(text=titulo, font_size='14sp'))
                    sw = Switch(active=cfg.get(key, False), size_hint_x=None, width=dp(80))
                    def make_toggle(k):
                        def togg(inst, val):
                            self.db.datos["configuracion"][k] = val
                            self.db.guardar_datos()
                        return togg
                    sw.bind(active=make_toggle(key))
                    op.add_widget(sw)
                    c.add_widget(op)
        
        btn_backup = ColoredButton(text="Crear Respaldo", bg_color=(0.22, 0, 0.7, 1))
        def backup(_):
            try:
                fn = f"backup_stay_focused_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
                with open(fn, 'w', encoding='utf-8') as f:
                    json.dump(self.db.datos, f, indent=2, ensure_ascii=False)
                self._msg(f"Respaldo: {fn}")
            except Exception as ex:
                self._msg(f"Error: {ex}", error=True)
        btn_backup.bind(on_press=backup)
        c.add_widget(btn_backup)
        
        c.add_widget(Label(text="INTERFAZ", font_size='14sp', bold=True, size_hint_y=None, height=dp(30),
                          color=(0.01, 0.85, 0.78, 1)))
        op = BoxLayout(orientation='horizontal', size_hint_y=None, height=dp(50), padding=dp(10))
        with op.canvas.before:
            Color(0.18, 0.18, 0.27, 1)
            r = RoundedRectangle(pos=op.pos, size=op.size, radius=[dp(8)])
        op.bind(pos=lambda *a: setattr(r, 'pos', op.pos), size=lambda *a: setattr(r, 'size', op.size))
        op.add_widget(Label(text="Modo Oscuro", font_size='14sp'))
        sw = Switch(active=cfg.get("modo_oscuro_activo", False), size_hint_x=None, width=dp(80))
        def togg_mo(inst, val):
            self.db.datos["configuracion"]["modo_oscuro_activo"] = val
            self.db.guardar_datos()
            self._msg("Reinicia la app para aplicar")
        sw.bind(active=togg_mo)
        op.add_widget(sw)
        c.add_widget(op)
        
        sv.add_widget(c)
        layout.add_widget(sv)
        self.add_widget(layout)
    
    def _msg(self, m, error=False):
        col = (0.69, 0, 0.13, 1) if error else (0.01, 0.85, 0.78, 1)
        c = BoxLayout(orientation='vertical', padding=dp(10))
        c.add_widget(Label(text=m))
        b = ColoredButton(text="OK", bg_color=col)
        p = Popup(title="Aviso", content=c, size_hint=(0.8, 0.3))
        b.bind(on_press=p.dismiss)
        c.add_widget(b)
        p.open()


class StayFocusedApp(App):
    title = "Stay Focused"
    
    def build(self):
        if ANDROID:
            try:
                request_permissions([Permission.RECEIVE_BOOT_COMPLETED, Permission.FOREGROUND_SERVICE])
            except Exception:
                pass
        self.db = BaseDatos()
        self.temporizadores = GestorTemporizadores()
        if not self.db.datos["estadisticas_uso"]:
            for app, mins in [("Instagram", 45), ("YouTube", 120), ("WhatsApp", 30), ("Facebook", 25), ("TikTok", 90)]:
                self.db.agregar_estadistica(app, mins)
        sm = ScreenManager()
        sm.add_widget(PantallaInicio(self))
        sm.add_widget(PantallaBloqueoApps(self))
        sm.add_widget(PantallaHorarios(self))
        sm.add_widget(PantallaEstadisticas(self))
        sm.add_widget(PantallaConfiguracion(self))
        return sm


if __name__ == '__main__':
    StayFocusedApp().run()
