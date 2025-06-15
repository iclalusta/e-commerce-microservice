from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.sql import func
from database import Base

class Notification(Base):
    __tablename__ = "notifications"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(String, nullable=False)
    order_id = Column(String, nullable=False)
    message = Column(String, nullable=False)
    status = Column(String, default="SENT")
    created_at = Column(DateTime(timezone=True), server_default=func.now())