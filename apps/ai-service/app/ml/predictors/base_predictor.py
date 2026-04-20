from abc import ABC, abstractmethod


class BasePredictor(ABC):

    @abstractmethod
    def predict(self, features: dict, is_main: bool):
        pass