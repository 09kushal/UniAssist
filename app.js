// UniAssist Interactive Showcase Logic
// Developed by Kushal Neupane - Oxford College of Engineering and Management

function switchScreen(screenId) {
    // Hide all screens
    const screens = document.querySelectorAll('.screen-view');
    screens.forEach(s => s.classList.remove('active'));

    // Show target screen
    const target = document.getElementById('screen-' + screenId);
    if (target) {
        target.classList.add('active');
    }

    // Update tab button states
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.classList.remove('active');
        if (tab.getAttribute('onclick') && tab.getAttribute('onclick').includes(screenId)) {
            tab.classList.add('active');
        }
    });

    // Update bottom nav states if applicable
    const navItems = document.querySelectorAll('.nav-item-icon');
    navItems.forEach(item => {
        item.classList.remove('active');
        if (item.getAttribute('onclick') && item.getAttribute('onclick').includes(screenId)) {
            item.classList.add('active');
        }
    });
}

function simulatePaymentSuccess() {
    const alertBox = document.getElementById('payment-status-message');
    const payBtn = document.querySelector('.btn-esewa-pay');

    payBtn.innerHTML = '<span>Processing eSewa HMAC-SHA256...</span>';
    payBtn.style.opacity = '0.7';

    setTimeout(() => {
        payBtn.innerHTML = '<span>✓ Payment Verified</span>';
        payBtn.style.background = '#059669';
        payBtn.style.opacity = '1';

        alertBox.style.display = 'block';
        alertBox.innerHTML = '🎉 <strong>Transaction Success!</strong><br>Booking confirmed! Live JaaS video link dispatched via FCM Notification.';

        setTimeout(() => {
            switchScreen('video');
        }, 1800);
    }, 1200);
}

// Auto update time in phone status bar
function updatePhoneClock() {
    const timeEls = document.querySelectorAll('.status-time');
    const now = new Date();
    let hours = now.getHours();
    const minutes = now.getMinutes().toString().padStart(2, '0');
    timeEls.forEach(el => {
        el.textContent = `${hours}:${minutes}`;
    });
}

document.addEventListener('DOMContentLoaded', () => {
    updatePhoneClock();
    setInterval(updatePhoneClock, 60000);
});
