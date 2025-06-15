import pika
import json
from sqlalchemy.orm import Session
import models, schemas

def process_order_created_event(db: Session, event_data: dict):
    """
    Gelen 'Order Created' olayını işler, bildirim oluşturur ve veritabanına kaydeder.
    """
    try:
        user_id = event_data.get("userId")
        order_id = event_data.get("orderId")
        
        if not user_id or not order_id:
            print(" [x] Hatalı olay verisi: userId veya orderId eksik.")
            return

        message = f"Sayın {user_id}, {order_id} numaralı siparişiniz başarıyla oluşturulmuştur."

        # Bildirimi veritabanına kaydet
        notification_data = schemas.NotificationCreate(
            user_id=str(user_id),
            order_id=str(order_id),
            message=message
        )
        db_notification = models.Notification(**notification_data.dict())
        db.add(db_notification)
        db.commit()
        db.refresh(db_notification)

        # --- BİLDİRİM GÖNDERME SİMÜLASYONU ---
        # Gerçek bir uygulamada burada e-posta, SMS veya push notification kodu olur.
        print("----------------------------------------------------")
        print(f"📧  Bildirim Gönderiliyor...")
        print(f"  Kime: {db_notification.user_id}")
        print(f"  Mesaj: {db_notification.message}")
        print(f"  Veritabanına kaydedildi. ID: {db_notification.id}")
        print("----------------------------------------------------")

    except Exception as e:
        print(f" [x] Olay işlenirken hata oluştu: {e}")


def start_consumer(db_session_factory):
    """RabbitMQ consumer'ını başlatır."""
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    channel = connection.channel()

    # 'orders' exchange'i ve 'notification_queue' kuyruğunu oluştur
    channel.exchange_declare(exchange='orders', exchange_type='fanout')
    result = channel.queue_declare(queue='', exclusive=True)
    queue_name = result.method.queue
    channel.queue_bind(exchange='orders', queue=queue_name)

    print(' [*] Bildirim servisi mesajları bekliyor. Çıkmak için CTRL+C basın.')

    def callback(ch, method, properties, body):
        db = db_session_factory()
        try:
            event_data = json.loads(body)
            print(f" [✔] 'Order Created' olayı alındı: {event_data}")
            process_order_created_event(db, event_data)
        finally:
            db.close()

    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=True)
    channel.start_consuming()