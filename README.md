# TankWar - Juego de Guerra de Tanques por Turnos

¡Bienvenido a TankWar! Un clásico juego de artillería por turnos para dos jugadores, desarrollado en Kotlin y Jetpack Compose para Android.

## Breve Explicación

**TankWar** es un juego por turnos donde dos tanques se enfrentan en un escenario 2D. El objetivo es simple: destruir al tanque enemigo antes de que él te destruya a ti. Para ganar, deberás ajustar cuidadosamente el ángulo y la potencia de tu disparo para que tu proyectil impacte al oponente, teniendo en cuenta la gravedad.

El juego está diseñado para 2 jugadores y ofrece dos modos principales:
* **Jugador vs Jugador (PVP):** Reta a un amigo en el mismo dispositivo.
* **Jugador vs IA (PVE):** Pon a prueba tu habilidad contra la IA del juego en tres niveles de dificultad: Fácil, Medio y Difícil.

## ¿Cómo Funciona?

El juego se desarrolla en rondas. El primer jugador en ganar **3 rondas** se corona como el campeón.

1.  **Inicio del Turno:** El jugador 1 comienza.
2.  **Apuntar:** El jugador en turno utiliza los controles deslizantes para ajustar el **ángulo** (0° a 90°) y la **potencia** (10 a 100) de su disparo.
3.  **Disparar:** Al presionar "¡DISPARAR!", el juego simula la física del proyectil, mostrando su trayectoria hasta que impacta con el enemigo o el terreno.
4.  **Daño:** Cada tanque comienza con 100 puntos de salud. Un impacto directo resta **25 puntos de salud**.
5.  **Cambio de Turno:** El turno pasa al siguiente jugador.
6.  **Fin de Ronda:** La ronda termina cuando la salud de un tanque llega a 0.
7.  **Guardar Partida:** ¿Necesitas irte? Puedes presionar el botón **"Guardar"** durante tu turno para guardar el progreso actual de la partida.
8.  **Cargar Partida:** Desde el menú principal, puedes seleccionar **"Cargar Juego"** para ver una lista de todas tus partidas guardadas y continuar donde lo dejaste.

## Pasos para Utilizar o Descargar

Este es un proyecto de código abierto de Android. Para compilarlo y ejecutarlo, necesitarás Android Studio.

1.  **Clonar el Repositorio:**
    ```bash
    git clone https://github.com/DLaVegaA/Actividad4_Desarrollo_juego_2_personas.git
    ```
2.  **Abrir en Android Studio:**
    * Abre Android Studio.
    * Selecciona `File > Open` (Archivo > Abrir).
    * Navega hasta la carpeta del proyecto que acabas de clonar y selecciónala.
3.  **Sincronizar Gradle:**
    * Android Studio detectará automáticamente el proyecto Gradle. Espera a que descargue todas las dependencias necesarias (indicado por una barra de progreso en la parte inferior).
4.  **Ejecutar la App:**
    * Una vez sincronizado, puedes ejecutar la aplicación.
    * Selecciona un emulador de Android o conecta un dispositivo físico.
    * Presiona el botón `Run 'app'` (Ejecutar 'app') (el ícono de play verde ▶️).

## 📸 Capturas del Funcionamiento


