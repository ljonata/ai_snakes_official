class Snake:
    # list of coordinates
    body = []
    maze_width = 0
    maze_height = 0
    
    # head is of type Coordinate
    # width and height are integers
    def __init__(self, head, width, height):
        self.body.append(head)
        self.maze_width = width
        self.maze_height = height
        
    # Check whether head collides with another snake
    # other_snake = body of the opponent's snake
    # Return whether this snake collides with another snake
    def headCollidesWith(self, other_snake):
        if self.body[0] in other_snake.body:
            return True
        else:
            return False
    
    # Move snake in the given direction
    # d is an instance of direction where should snake crawl to
    # grow is boolean, whether the snake eat an apple
    # Return False if this snake collides with itself or maze bounds
    def moveTo(self, direction, grow):
        head_copy = Coordinate(self.body[0].x, self.body[0].y)
        new_head = head_copy.move_to(direction)

        if not new_head.in_bounds():
            return False # Left maze

        if not grow:
            del self.body[-1]
        
        if new_head in self.body:
            return False # Collided with itself

        body.insert(0, new_head)
        return True