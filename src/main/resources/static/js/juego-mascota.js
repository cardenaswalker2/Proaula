document.addEventListener("DOMContentLoaded", function() {
    // Inicializar Tooltips de Bootstrap para que funcionen las ayudas
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // --- ESTADO INICIAL DEL JUEGO ---
    const gameState = {
        health: 100,
        hunger: 0,
        happiness: 100,
        energy: 100,
        isSleeping: false,
        gameTime: 480, // Inicia a las 08:00
        day: 1,
        lastUpdate: Date.now()
    };

    // --- ELEMENTOS DEL DOM ---
    const dom = {
        healthBar: document.getElementById('health-bar'),
        hungerBar: document.getElementById('hunger-bar'),
        happinessBar: document.getElementById('happiness-bar'),
        energyBar: document.getElementById('energy-bar'),
        petImage: document.getElementById('pet-image'),
        petStatusText: document.getElementById('pet-status-text'),
        timeDisplay: document.getElementById('time-display'),
        dayPhase: document.getElementById('day-phase'),
        dayCounter: document.getElementById('day-counter'),
        petEnvironment: document.getElementById('pet-environment'),
        eventLog: document.getElementById('event-log'),
        thoughtBubble: document.getElementById('thought-bubble'),
        actionButtons: document.querySelectorAll('.btn-action')
    };

    // --- ENLACES DE IMÁGENES ---
    const petImages = {
        happy: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8OHx8cGVycm8lMjBmZWxpenxlbnwwfHwwfHx8MA%3D%3D&fm=jpg&q=60&w=3000',
        neutral: 'https://i.pinimg.com/474x/d3/78/a1/d378a1b5845895da59d4b4154b280a8d.jpg',
        sad: 'https://cdn.dogsplanet.com/wp-content/uploads/2021/02/perro-triste.jpg',
        sleeping: 'https://www.zoopinto.es/wp-content/uploads/2020/10/cuanto-duerme-un-perro.jpg'
    };

    // --- FUNCIONES BÁSICAS DEL JUEGO ---

    function logEvent(message, type = 'info') {
        const logEntry = document.createElement('p');
        const time = formatTime(gameState.gameTime);
        logEntry.innerHTML = `<strong>[${time}]</strong> ${message}`;
        if (type === 'warn') logEntry.style.color = '#fd7e14';
        if (type === 'danger') logEntry.style.color = '#dc3545';
        dom.eventLog.prepend(logEntry);
        if (dom.eventLog.children.length > 20) {
            dom.eventLog.lastChild.remove();
        }
    }

    function updateBars() {
        const updateBar = (bar, value, lowIsGood = false) => {
            bar.style.width = `${value}%`;
            bar.textContent = `${Math.round(value)}%`;
            let colorClass = 'bg-success';
            if ((!lowIsGood && value < 30) || (lowIsGood && value > 80)) colorClass = 'bg-danger';
            else if ((!lowIsGood && value < 60) || (lowIsGood && value > 50)) colorClass = 'bg-warning';
            bar.className = 'progress-bar';
            bar.classList.add(colorClass);
        };
        updateBar(dom.healthBar, gameState.health);
        updateBar(dom.hungerBar, gameState.hunger, true);
        updateBar(dom.happinessBar, gameState.happiness);
        updateBar(dom.energyBar, gameState.energy);
    }

    function formatTime(totalMinutes) {
        const h = Math.floor(totalMinutes / 60) % 24;
        const m = Math.floor(totalMinutes % 60);
        return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    }

    function setPetImage(newSrc) {
        if (dom.petImage.src === newSrc) return;
        dom.petImage.classList.add('pet-changing');
        setTimeout(() => {
            dom.petImage.src = newSrc;
            dom.petImage.onload = () => {
                dom.petImage.classList.remove('pet-changing');
            };
        }, 500);
    }

    let thoughtBubbleTimeout;
    function showThought(text) {
        clearTimeout(thoughtBubbleTimeout);
        dom.thoughtBubble.textContent = text;
        dom.thoughtBubble.classList.add('visible');
        thoughtBubbleTimeout = setTimeout(() => {
            dom.thoughtBubble.classList.remove('visible');
        }, 4000);
    }
    
    // --- FUNCIONES PRINCIPALES (ACTUALIZACIÓN DE ESTADO) ---
    
    function updateTimeAndEnvironment() {
        dom.timeDisplay.textContent = formatTime(gameState.gameTime);
        dom.dayCounter.textContent = `Día ${gameState.day}`;
        const hour = Math.floor(gameState.gameTime / 60) % 24;

        if (hour >= 5 && hour < 12) { dom.dayPhase.textContent = "Mañana"; dom.petEnvironment.style.background = 'linear-gradient(to top, #87CEEB, #a8e0f7)'; } 
        else if (hour >= 12 && hour < 18) { dom.dayPhase.textContent = "Tarde"; dom.petEnvironment.style.background = 'linear-gradient(to top, #4682B4, #639cce)'; } 
        else if (hour >= 18 && hour < 22) { dom.dayPhase.textContent = "Atardecer"; dom.petEnvironment.style.background = 'linear-gradient(to top, #d94e2a, #ff7f50)'; } 
        else { dom.dayPhase.textContent = "Noche"; dom.petEnvironment.style.background = 'linear-gradient(to top, #0d1b2a, #243b55)'; }
    }

    function updatePetStatus() {
        dom.petImage.classList.toggle('pet-idle', !gameState.isSleeping);

        if (gameState.isSleeping) {
            setPetImage(petImages.sleeping);
            dom.petStatusText.textContent = "Rocky duerme plácidamente...";
            dom.thoughtBubble.classList.remove('visible');
        } else if (gameState.health <= 30) {
            setPetImage(petImages.sad);
            dom.petStatusText.textContent = "Rocky no se siente bien.";
            showThought("Necesito un doctor...");
        } else if (gameState.hunger >= 80) {
            setPetImage(petImages.sad);
            dom.petStatusText.textContent = "Rocky tiene mucha hambre.";
            showThought("¡Mi pancita ruge!");
        } else if (gameState.happiness <= 20) {
            setPetImage(petImages.sad);
            dom.petStatusText.textContent = "Rocky está muy triste.";
            showThought("¿Nadie quiere jugar conmigo?");
        } else if (gameState.energy <= 20) {
            setPetImage(petImages.neutral);
            dom.petStatusText.textContent = "Rocky parece cansado.";
            showThought("Necesito una siesta...");
        } else if (gameState.happiness <= 50) {
            setPetImage(petImages.neutral);
            dom.petStatusText.textContent = "Rocky parece estar bien.";
        } else {
            setPetImage(petImages.happy);
            dom.petStatusText.textContent = "¡Rocky está radiante de felicidad!";
        }
    }
    
    // --- BUCLE PRINCIPAL DEL JUEGO ---
    function gameLoop() {
        const now = Date.now();
        const delta = (now - gameState.lastUpdate) / 1000;
        gameState.lastUpdate = now;
        const previousTime = gameState.gameTime;

        if (!gameState.isSleeping) {
            gameState.gameTime += delta * 10;
            gameState.hunger = Math.min(100, gameState.hunger + delta * 0.8);
            gameState.energy = Math.max(0, gameState.energy - delta * 0.6);
            if (gameState.hunger > 60) gameState.happiness = Math.max(0, gameState.happiness - delta * 0.7);
            if (gameState.energy < 20) gameState.happiness = Math.max(0, gameState.happiness - delta * 0.3);
        } else {
            gameState.energy = Math.min(100, gameState.energy + delta * 10);
            if (gameState.energy >= 100) {
                gameState.isSleeping = false;
                dom.actionButtons.forEach(btn => btn.disabled = false);
                document.getElementById('sleep-button').blur();
                logEvent("Rocky se ha despertado lleno de energía.", "info");
                showThought("¡Qué buena siesta!");
            }
        }
        
        if (gameState.hunger > 80) gameState.health = Math.max(0, gameState.health - delta * 0.5);

        if (Math.floor(previousTime / 1440) < Math.floor(gameState.gameTime / 1440)) {
            gameState.day++;
            logEvent(`¡Ha comenzado el día ${gameState.day}!`, "info");
        }

        updateBars();
        updateTimeAndEnvironment();
        updatePetStatus();

        requestAnimationFrame(gameLoop);
    }
    
    // --- ACCIONES DEL JUGADOR ---
    document.querySelectorAll('.btn-action').forEach(button => {
        button.addEventListener('click', (e) => {
            e.currentTarget.blur();
        });
    });
    
    document.getElementById('feed-button').addEventListener('click', () => {
        if (gameState.hunger > 10) {
            gameState.hunger = Math.max(0, gameState.hunger - 30);
            gameState.health = Math.min(100, gameState.health + 5);
            logEvent("Has alimentado a Rocky. ¡Qué rico!");
            showThought("¡Delicioso!");
        } else {
            logEvent("Rocky no tiene hambre ahora mismo.", "warn");
        }
    });

    document.getElementById('play-button').addEventListener('click', () => {
        if (gameState.energy > 20) {
            gameState.happiness = Math.min(100, gameState.happiness + 25);
            gameState.hunger = Math.min(100, gameState.hunger + 10);
            gameState.energy = Math.max(0, gameState.energy - 15);
            logEvent("¡Qué divertido! A Rocky le encanta jugar.");
            showThought("¡Yupi, a jugar!");
        } else {
            logEvent("Rocky está muy cansado para jugar.", "warn");
        }
    });
    
    document.getElementById('heal-button').addEventListener('click', () => {
        if (gameState.health < 100) {
            gameState.health = Math.min(100, gameState.health + 40);
            gameState.happiness = Math.max(0, gameState.happiness - 10);
            logEvent("Has curado a Rocky. Se siente mucho mejor.", "info");
            showThought("Esa medicina sabe rara...");
        } else {
            logEvent("Rocky ya está completamente sano.", "info");
        }
    });
    
    document.getElementById('sleep-button').addEventListener('click', () => {
         if (!gameState.isSleeping) {
            gameState.isSleeping = true;
            dom.actionButtons.forEach(btn => btn.disabled = true);
            logEvent("Rocky se ha ido a dormir. Zzz...", "info");
         }
    });

    // --- INICIAR JUEGO ---
    logEvent("¡Bienvenido al simulador! Cuida bien de Rocky.");
    dom.petImage.src = petImages.happy; 
    requestAnimationFrame(gameLoop);
});