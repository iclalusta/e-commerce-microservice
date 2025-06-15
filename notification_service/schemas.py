from pydantic import BaseModel
from datetime import datetime

class NotificationBase(BaseModel):
    user_id: str
    order_id: str
    message: str

class NotificationCreate(NotificationBase):
    pass

class NotificationSchema(NotificationBase):
    id: int
    status: str
    created_at: datetime

    class Config:
        orm_mode = True