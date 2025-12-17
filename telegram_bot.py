import logging
import hashlib
import random
import string
from datetime import datetime, timedelta
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import Application, CommandHandler, CallbackQueryHandler, MessageHandler, filters, ContextTypes

BOT_TOKEN = "7996610464:AAHMIs2CwF0--eB4_8S4X1-C5b5kZRYNQMs"
ADMIN_IDS = [6466581970]
SECRET_KEY = "OtoServiceMaster2025SecretKey"

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)
logger = logging.getLogger(__name__)

licenses_db = {}
users_db = {}
used_master_licenses = set()

def generate_master_signature(date_str: str) -> str:
    data = f"MASTER-{date_str}-{SECRET_KEY}"
    hash_obj = hashlib.sha256(data.encode())
    return hash_obj.hexdigest()[:8].upper()

def generate_master_license(days: int = None, single_use: bool = False) -> tuple:
    if days is None:
        date_str = "UNLIMITED"
        expiry = None
    else:
        expiry_date = datetime.now() + timedelta(days=days)
        date_str = expiry_date.strftime("%Y%m%d")
        expiry = expiry_date
    
    signature = generate_master_signature(date_str)
    
    random_part = ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))
    
    use_type = "SINGLE" if single_use else "MULTI"
    
    license_code = f"MASTER-{date_str}-{signature}-{use_type}-{random_part}"
    
    return license_code, expiry, single_use

def generate_normal_license(device_id: str, days: int) -> tuple:
    expiry_date = datetime.now() + timedelta(days=days)
    device_hash = hashlib.md5(device_id.encode()).hexdigest()[:8].upper()
    suffix = ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))
    random_part = ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))
    license_code = f"{device_hash}-{suffix}-{random_part}"
    return license_code, expiry_date

def is_admin(user_id: int) -> bool:
    return user_id in ADMIN_IDS

def format_license_info(license_code: str, license_type: str, days: int = None, single_use: bool = False) -> str:
    if license_type == "master":
        use_info = "🔴 Tek Kullanımlık" if single_use else "🟢 Çoklu Kullanım"
        if days is None:
            duration = "♾️ Süresiz"
        else:
            expiry_date = datetime.now() + timedelta(days=days)
            duration = f"⏱ {days} gün (Son: {expiry_date.strftime('%d.%m.%Y')})"
        
        return (
            "🔑 *MASTER LİSANS*\n\n"
            f"📋 Lisans Kodu:\n`{license_code}`\n\n"
            f"📊 Kullanım: {use_info}\n"
            f"📅 Süre: {duration}\n\n"
            "✅ Tüm güvenlik bypass\n"
            "✅ Herhangi bir cihazda\n"
            "✅ Tam yetki"
        )
    else:
        expiry_date = datetime.now() + timedelta(days=days)
        return (
            "📋 *NORMAL LİSANS*\n\n"
            f"📋 Lisans Kodu:\n`{license_code}`\n\n"
            f"⏱ Süre: {days} gün\n"
            f"📅 Son Kullanma: {expiry_date.strftime('%d.%m.%Y')}\n"
            "📱 Cihaza Özel: Evet"
        )

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    users_db[user.id] = {
        'username': user.username,
        'first_name': user.first_name,
        'joined': datetime.now()
    }
    
    keyboard = [
        [InlineKeyboardButton("🔑 Master Lisans Oluştur", callback_data='create_master')],
        [InlineKeyboardButton("📋 Normal Lisans Oluştur", callback_data='get_normal')],
        [InlineKeyboardButton("📜 Lisanslarım", callback_data='my_licenses')],
        [InlineKeyboardButton("❓ Yardım", callback_data='help')],
    ]
    
    if is_admin(user.id):
        keyboard.append([InlineKeyboardButton("👑 Admin Panel", callback_data='admin')])
    
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    message_text = (
        f"🤖 *OtoService Lisans Botu*\n\n"
        f"Merhaba {user.first_name}!\n\n"
        f"🔑 *Master Lisans* - Dinamik, özelleştirilebilir\n"
        f"📋 *Normal Lisans* - Cihaza özel, süreli\n\n"
        f"Ne yapmak istersiniz?"
    )
    
    await update.message.reply_text(
        message_text,
        reply_markup=reply_markup,
        parse_mode='Markdown'
    )

async def create_master_menu(update: Update, context: ContextTypes.DEFAULT_TYPE, query=None):
    keyboard = [
        [InlineKeyboardButton("♾️ Süresiz", callback_data='master_unlimited')],
        [InlineKeyboardButton("📅 30 Gün", callback_data='master_30')],
        [InlineKeyboardButton("📅 90 Gün", callback_data='master_90')],
        [InlineKeyboardButton("📅 180 Gün", callback_data='master_180')],
        [InlineKeyboardButton("📅 365 Gün", callback_data='master_365')],
        [InlineKeyboardButton("🔧 Özel Süre", callback_data='master_custom')],
        [InlineKeyboardButton("« Geri", callback_data='back_to_main')]
    ]
    
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    message_text = (
        "🔑 *Master Lisans Oluştur*\n\n"
        "Lütfen lisans süresini seçin:\n\n"
        "♾️ *Süresiz* - Hiç dolmaz\n"
        "📅 *Süreli* - Belirli gün sonra dolar\n"
        "🔧 *Özel* - Kendin belirle\n\n"
        "Her lisans *tek kullanımlık* veya *çoklu kullanım* olabilir."
    )
    
    if query:
        await query.edit_message_text(message_text, reply_markup=reply_markup, parse_mode='Markdown')
    else:
        await update.message.reply_text(message_text, reply_markup=reply_markup, parse_mode='Markdown')

async def master_usage_menu(update: Update, context: ContextTypes.DEFAULT_TYPE, query, days_key: str):
    keyboard = [
        [InlineKeyboardButton("🔴 Tek Kullanımlık", callback_data=f'{days_key}_single')],
        [InlineKeyboardButton("🟢 Çoklu Kullanım", callback_data=f'{days_key}_multi')],
        [InlineKeyboardButton("« Geri", callback_data='create_master')]
    ]
    
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    message_text = (
        "🔧 *Kullanım Tipi Seçin*\n\n"
        "🔴 *Tek Kullanımlık*\n"
        "   • Bir kez kullanılır\n"
        "   • Tekrar kullanılamaz\n"
        "   • Daha güvenli\n\n"
        "🟢 *Çoklu Kullanım*\n"
        "   • Sınırsız kullanım\n"
        "   • Paylaşılabilir\n"
        "   • Esnek"
    )
    
    await query.edit_message_text(message_text, reply_markup=reply_markup, parse_mode='Markdown')

async def button_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()
    user_id = query.from_user.id
    
    if query.data == 'create_master':
        if not is_admin(user_id):
            await query.edit_message_text(
                "❌ Master lisans sadece adminler oluşturabilir!\n\n"
                "Normal lisans için /normal komutunu kullanın.",
                parse_mode='Markdown'
            )
            return
        await create_master_menu(None, context, query)
    
    elif query.data == 'master_unlimited':
        await master_usage_menu(update, context, query, 'unlimited')
    
    elif query.data == 'master_30':
        await master_usage_menu(update, context, query, 'days_30')
    
    elif query.data == 'master_90':
        await master_usage_menu(update, context, query, 'days_90')
    
    elif query.data == 'master_180':
        await master_usage_menu(update, context, query, 'days_180')
    
    elif query.data == 'master_365':
        await master_usage_menu(update, context, query, 'days_365')
    
    elif query.data == 'master_custom':
        await query.edit_message_text(
            "📅 *Özel Süre Belirle*\n\n"
            "Kaç gün geçerli olsun?\n\n"
            "Örnek: `90` yazın",
            parse_mode='Markdown'
        )
        context.user_data['waiting_for_custom_days'] = True
    
    elif query.data.startswith('unlimited_'):
        single_use = query.data.endswith('_single')
        license_code, expiry, is_single = generate_master_license(None, single_use)
        
        license_info = {
            'code': license_code,
            'type': 'master',
            'user_id': user_id,
            'created': datetime.now(),
            'expires': expiry,
            'single_use': is_single,
            'used': False
        }
        licenses_db[license_code] = license_info
        
        message = format_license_info(license_code, 'master', None, is_single)
        await query.edit_message_text(message, parse_mode='Markdown')
    
    elif query.data.startswith('days_'):
        parts = query.data.split('_')
        if len(parts) == 3:
            days = int(parts[1])
            single_use = parts[2] == 'single'
            
            license_code, expiry, is_single = generate_master_license(days, single_use)
            
            license_info = {
                'code': license_code,
                'type': 'master',
                'user_id': user_id,
                'created': datetime.now(),
                'expires': expiry,
                'single_use': is_single,
                'used': False
            }
            licenses_db[license_code] = license_info
            
            message = format_license_info(license_code, 'master', days, is_single)
            await query.edit_message_text(message, parse_mode='Markdown')
    
    elif query.data == 'get_normal':
        await query.edit_message_text(
            "📋 *Normal Lisans*\n\n"
            "Device ID ve süre gönderin:\n\n"
            "Örnek:\n"
            "`abc123def456`\n"
            "`30`",
            parse_mode='Markdown'
        )
        context.user_data['waiting_for_license_info'] = True
    
    elif query.data == 'my_licenses':
        user_licenses = [lic for lic in licenses_db.values() if lic['user_id'] == user_id]
        
        if not user_licenses:
            await query.edit_message_text("❌ Henüz lisansınız yok.")
            return
        
        message = "📋 *Lisanslarınız:*\n\n"
        for lic in user_licenses:
            if lic['type'] == 'master':
                use_status = "✅ Kullanılmadı" if not lic.get('used', False) else "❌ Kullanıldı"
                status = "✅" if lic['expires'] is None or lic['expires'] > datetime.now() else "❌"
                message += (
                    f"{status} `{lic['code']}`\n"
                    f"📌 MASTER\n"
                    f"🔄 {use_status}\n"
                    f"📅 {lic['created'].strftime('%d.%m.%Y')}\n\n"
                )
            else:
                status = "✅" if lic['expires'] > datetime.now() else "❌"
                message += (
                    f"{status} `{lic['code']}`\n"
                    f"📌 NORMAL\n"
                    f"📅 {lic['created'].strftime('%d.%m.%Y')}\n\n"
                )
        
        await query.edit_message_text(message, parse_mode='Markdown')
    
    elif query.data == 'help':
        help_text = (
            "📖 *YARDIM*\n\n"
            "*Master Lisans:*\n"
            "• Dinamik oluşturma\n"
            "• Süresiz veya süreli\n"
            "• Tek kullanımlık veya çoklu\n"
            "• Tüm güvenlik bypass\n\n"
            "*Normal Lisans:*\n"
            "• Cihaza özel\n"
            "• Süreli kullanım\n"
            "• Standart güvenlik"
        )
        await query.edit_message_text(help_text, parse_mode='Markdown')
    
    elif query.data == 'admin':
        if not is_admin(user_id):
            await query.edit_message_text("❌ Bu özellik sadece adminler için!")
            return
        
        total_users = len(users_db)
        total_licenses = len(licenses_db)
        master_licenses = sum(1 for lic in licenses_db.values() if lic['type'] == 'master')
        normal_licenses = total_licenses - master_licenses
        
        message = (
            "👑 *ADMİN PANEL*\n\n"
            "📊 *İstatistikler:*\n"
            f"👥 Kullanıcı: {total_users}\n"
            f"🔑 Lisans: {total_licenses}\n"
            f"  ├─ 🔑 Master: {master_licenses}\n"
            f"  └─ 📋 Normal: {normal_licenses}\n\n"
            "*Komutlar:*\n"
            "/master - Master lisans oluştur\n"
            "/stats - Detaylı istatistikler"
        )
        
        await query.edit_message_text(message, parse_mode='Markdown')
    
    elif query.data == 'back_to_main':
        await start(query, context)

async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if context.user_data.get('waiting_for_custom_days'):
        try:
            days = int(update.message.text.strip())
            if days < 1 or days > 3650:
                await update.message.reply_text("❌ Geçersiz süre! 1-3650 arası gün girin.")
                return
            
            keyboard = [
                [InlineKeyboardButton("🔴 Tek Kullanımlık", callback_data=f'days_{days}_single')],
                [InlineKeyboardButton("🟢 Çoklu Kullanım", callback_data=f'days_{days}_multi')],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            
            await update.message.reply_text(
                f"✅ Süre: {days} gün\n\nKullanım tipini seçin:",
                reply_markup=reply_markup
            )
            context.user_data['waiting_for_custom_days'] = False
        except:
            await update.message.reply_text("❌ Geçersiz sayı!")
    
    elif context.user_data.get('waiting_for_license_info'):
        text = update.message.text.strip()
        lines = text.split('\n')
        
        if len(lines) < 2:
            await update.message.reply_text("❌ Eksik bilgi! 2 satır:\n1. Device ID\n2. Süre (gün)")
            return
        
        device_id = lines[0].strip()
        try:
            days = int(lines[1].strip())
            if days < 1 or days > 365:
                raise ValueError()
        except:
            await update.message.reply_text("❌ Geçersiz süre! 1-365 arası")
            return
        
        license_code, expiry_date = generate_normal_license(device_id, days)
        license_info = {
            'code': license_code,
            'type': 'normal',
            'user_id': update.effective_user.id,
            'device_id': device_id,
            'created': datetime.now(),
            'expires': expiry_date
        }
        licenses_db[license_code] = license_info
        
        message = format_license_info(license_code, 'normal', days)
        await update.message.reply_text(message, parse_mode='Markdown')
        context.user_data['waiting_for_license_info'] = False
    else:
        await update.message.reply_text("/help veya /start")

def main():
    application = Application.builder().token(BOT_TOKEN).build()
    
    application.add_handler(CommandHandler("start", start))
    application.add_handler(CallbackQueryHandler(button_callback))
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))
    
    print("🤖 Bot başlatıldı!")
    print(f"📋 Bot Token: {BOT_TOKEN[:20]}...")
    print(f"👑 Admin ID: {ADMIN_IDS}")
    print(f"🔑 Master Lisans: DİNAMİK")
    print("\n✅ Bot çalışıyor... (Durdurmak için Ctrl+C)")
    
    application.run_polling(allowed_updates=Update.ALL_TYPES)

if __name__ == '__main__':
    main()
